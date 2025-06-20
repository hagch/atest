package io.webcontify.collection.schema

import java.time.OffsetDateTime

enum class FieldType {
    TEXT,
    INTEGER,
    BOOLEAN,
    UUID,
    DATE,
    TIMESTAMP
}

data class FieldMetadata(
    val id: Long?,
    val collectionId: Long,
    val name: String,
    val type: FieldType,
    val nullable: Boolean,
    val unique: Boolean,
    val defaultValue: String? = null,
    val enumValues: List<String>? = null,
    val createdAt: OffsetDateTime? = null
)
