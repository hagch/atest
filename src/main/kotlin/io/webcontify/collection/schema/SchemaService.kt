package io.webcontify.collection.schema

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class SchemaService(private val dsl: DSLContext) {

    @Transactional
    fun createCollection(name: String, idGeneration: IdGeneration = IdGeneration.SEQUENCE): CollectionMetadata {
        val exists = dsl.fetchCount(DSL.table("collections"), DSL.field("name").eq(name)) > 0
        require(!exists) { "collection $name already exists" }

        val id = dsl.insertInto(DSL.table("collections"))
            .columns(DSL.field("name"), DSL.field("id_generation"))
            .values(name, idGeneration.name)
            .returningResult(DSL.field("id"))
            .fetchOne()!![0] as Long

        val create = dsl.createTable(DSL.name(name))
        when (idGeneration) {
            IdGeneration.SEQUENCE -> create.column("id", SQLDataType.BIGINT.identity(true))
            IdGeneration.UUID -> create.column("id", SQLDataType.UUID.nullable(false).defaultValue(DSL.field("gen_random_uuid()", SQLDataType.UUID)))
        }
        create.constraint(DSL.primaryKey("id")).execute()
        return CollectionMetadata(id, name, idGeneration, OffsetDateTime.now())
    }

    @Transactional(readOnly = true)
    fun listCollections(): List<CollectionMetadata> =
        dsl.select(DSL.field("id"), DSL.field("name"), DSL.field("id_generation"))
            .from("collections")
            .fetch {
                CollectionMetadata(
                    it.get("id", Long::class.java),
                    it.get("name", String::class.java),
                    IdGeneration.valueOf(it.get("id_generation", String::class.java)),
                    it.get("created_at", OffsetDateTime::class.java)
                )
            }

    @Transactional
    fun addField(
        collectionId: Long,
        name: String,
        type: FieldType,
        nullable: Boolean = true,
        unique: Boolean = false,
        defaultValue: String? = null,
        enumValues: List<String>? = null
    ): FieldMetadata {
        val collectionName = getCollectionName(collectionId)
        val id = dsl.insertInto(DSL.table("fields"))
            .columns(
                DSL.field("collection_id"),
                DSL.field("name"),
                DSL.field("type"),
                DSL.field("nullable"),
                DSL.field("unique"),
                DSL.field("default_value"),
                DSL.field("enum_values")
            ).values(
                collectionId,
                name,
                type.name,
                nullable,
                unique,
                defaultValue,
                enumValues?.joinToString(",")
            ).returningResult(DSL.field("id")).fetchOne()!![0] as Long

        val dataType = when (type) {
            FieldType.TEXT -> SQLDataType.CLOB
            FieldType.INTEGER -> SQLDataType.BIGINT
            FieldType.BOOLEAN -> SQLDataType.BOOLEAN
            FieldType.UUID -> SQLDataType.UUID
            FieldType.DATE -> SQLDataType.DATE
            FieldType.TIMESTAMP -> SQLDataType.TIMESTAMPWITHTIMEZONE
        }.nullable(nullable)

        val column = DSL.field(DSL.name(name), dataType)
        dsl.alterTable(DSL.name(collectionName)).addColumn(column).execute()
        if (defaultValue != null) {
            dsl.alterTable(DSL.name(collectionName))
                .alterColumn(name).default_(DSL.inline(defaultValue)).execute()
        }
        if (enumValues != null && enumValues.isNotEmpty()) {
            val constraint = DSL.constraint("chk_${collectionName}_${name}_enum")
                .check(DSL.field(DSL.name(name)).`in`(*enumValues.toTypedArray()))
            dsl.alterTable(DSL.name(collectionName)).add(constraint).execute()
        }
        if (unique) {
            val constraint = DSL.constraint("uk_${collectionName}_${name}").unique(DSL.field(DSL.name(name)))
            dsl.alterTable(DSL.name(collectionName)).add(constraint).execute()
        }
        return FieldMetadata(id, collectionId, name, type, nullable, unique, defaultValue, enumValues)
    }

    @Transactional
    fun addRelation(meta: RelationMetadata): RelationMetadata {
        val id = dsl.insertInto(DSL.table("relations"))
            .columns(
                DSL.field("source_collection_id"),
                DSL.field("target_collection_id"),
                DSL.field("source_field_ids"),
                DSL.field("target_field_ids"),
                DSL.field("type")
            ).values(
                meta.sourceCollectionId,
                meta.targetCollectionId,
                meta.sourceFieldIds.joinToString(","),
                meta.targetFieldIds.joinToString(","),
                meta.type.name
            ).returningResult(DSL.field("id")).fetchOne()!![0] as Long

        if (meta.type == RelationType.MANY_TO_ONE || meta.type == RelationType.ONE_TO_ONE) {
            val source = getCollectionName(meta.sourceCollectionId)
            val target = getCollectionName(meta.targetCollectionId)
            val sourceFields = meta.sourceFieldIds.map { DSL.field(DSL.name(getFieldName(it))) }
            val targetFields = meta.targetFieldIds.map { DSL.name(getFieldName(it)) }
            val constraint = DSL.constraint("fk_${source}_${id}")
                .foreignKey(*sourceFields.toTypedArray())
                .references(DSL.name(target), *targetFields.toTypedArray())
            dsl.alterTable(DSL.name(source)).add(constraint).execute()
        }
        return meta.copy(id = id)
    }

    fun getCollectionName(id: Long): String =
        dsl.select(DSL.field("name"))
            .from("collections")
            .where(DSL.field("id").eq(id))
            .fetchOne()!!.get("name", String::class.java)

    fun getFields(collectionId: Long): List<FieldMetadata> =
        dsl.selectFrom("fields").where(DSL.field("collection_id").eq(collectionId)).fetch {
            FieldMetadata(
                it.get("id", Long::class.java),
                collectionId,
                it.get("name", String::class.java),
                FieldType.valueOf(it.get("type", String::class.java)),
                it.get("nullable", Boolean::class.java),
                it.get("unique", Boolean::class.java),
                it.get("default_value", String::class.java),
                it.get("enum_values", String::class.java)?.split(","),
                it.get("created_at", OffsetDateTime::class.java)
            )
        }

    private fun getFieldName(id: Long): String =
        dsl.select(DSL.field("name"))
            .from("fields")
            .where(DSL.field("id").eq(id))
            .fetchOne()!!.get("name", String::class.java)
}
