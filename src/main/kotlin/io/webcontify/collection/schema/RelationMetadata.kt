package io.webcontify.collection.schema

import java.time.OffsetDateTime

data class RelationMetadata(
    val id: Long?,
    val sourceCollectionId: Long,
    val targetCollectionId: Long,
    val sourceFieldIds: List<Long>,
    val targetFieldIds: List<Long>,
    val type: RelationType,
    val createdAt: OffsetDateTime? = null
)
