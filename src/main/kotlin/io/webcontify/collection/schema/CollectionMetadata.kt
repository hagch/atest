package io.webcontify.collection.schema

import java.time.OffsetDateTime

data class CollectionMetadata(
    val id: Long?,
    val name: String,
    val idGeneration: IdGeneration,
    val createdAt: OffsetDateTime? = null
)
