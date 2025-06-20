package io.webcontify.collection.item

import io.webcontify.collection.schema.FieldMetadata
import io.webcontify.collection.schema.SchemaService
import org.jooq.DSLContext
import org.jooq.Condition
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ItemService(
    private val dsl: DSLContext,
    private val schemaService: SchemaService
) {

    @Transactional
    fun createItem(collectionId: Long, values: Map<String, Any?>): Long {
        val collectionName = schemaService.getCollectionName(collectionId)
        val meta = schemaService.getFields(collectionId).associateBy { it.name }
        validateValues(values, meta)

        val insertMap = meta.values.associate { field ->
            val value = values[field.name] ?: field.defaultValue
            field.name to value
        }

        val table = DSL.table(DSL.name(collectionName))
        val fields = insertMap.keys.map { DSL.field(DSL.name(it)) }
        val record = dsl.insertInto(table)
            .columns(fields)
            .values(insertMap.values.toList())
            .returningResult(DSL.field("id"))
            .fetchOne()!![0] as Long
        return record
    }

    @Transactional(readOnly = true)
    fun findItem(collectionId: Long, id: Long): Map<String, Any?>? {
        val table = DSL.table(DSL.name(schemaService.getCollectionName(collectionId)))
        val result = dsl.selectFrom(table).where(DSL.field("id").eq(id)).fetchOne() ?: return null
        return result.intoMap()
    }

    @Transactional
    fun updateItem(collectionId: Long, id: Long, values: Map<String, Any?>) {
        val collectionName = schemaService.getCollectionName(collectionId)
        val meta = schemaService.getFields(collectionId).associateBy { it.name }
        validateValues(values, meta, partial = true, id = id, tableName = collectionName)

        val table = DSL.table(DSL.name(collectionName))
        val fieldMap = values.mapKeys { DSL.field(DSL.name(it.key)) }
        dsl.update(table)
            .set(fieldMap)
            .where(DSL.field("id").eq(id))
            .execute()
    }

    @Transactional
    fun deleteItem(collectionId: Long, id: Long) {
        val table = DSL.table(DSL.name(schemaService.getCollectionName(collectionId)))
        dsl.deleteFrom(table).where(DSL.field("id").eq(id)).execute()
    }

    @Transactional(readOnly = true)
    fun search(collectionId: Long, criteria: Map<String, Any?>): List<Map<String, Any?>> {
        val table = DSL.table(DSL.name(schemaService.getCollectionName(collectionId)))
        var condition: Condition = DSL.trueCondition()
        criteria.forEach { (k, v) -> condition = condition.and(DSL.field(DSL.name(k)).eq(v)) }
        return dsl.selectFrom(table).where(condition).fetch().map { it.intoMap() }
    }

    private fun validateValues(
        values: Map<String, Any?>,
        meta: Map<String, FieldMetadata>,
        partial: Boolean = false,
        id: Long? = null,
        tableName: String? = null
    ) {
        meta.values.forEach { field ->
            if (!partial || values.containsKey(field.name)) {
                val value = values[field.name]
                if (!field.nullable && value == null) {
                    throw IllegalArgumentException("${field.name} cannot be null")
                }
                if (field.enumValues != null && value != null && value !in field.enumValues!!) {
                    throw IllegalArgumentException("${field.name} must be one of ${field.enumValues}")
                }
                if (field.unique && value != null && tableName != null) {
                    val cond = if (id != null) {
                        DSL.field(DSL.name("id")).ne(id).and(DSL.field(DSL.name(field.name)).eq(value))
                    } else {
                        DSL.field(DSL.name(field.name)).eq(value)
                    }
                    val count = dsl.fetchCount(DSL.table(DSL.name(tableName)), cond)
                    if (count > 0) throw IllegalArgumentException("${field.name} must be unique")
                }
            }
        }
    }
}
