package io.webcontify.collection.web

import io.webcontify.collection.schema.RelationMetadata
import io.webcontify.collection.schema.RelationType
import io.webcontify.collection.schema.SchemaService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/relations")
class RelationController(private val schemaService: SchemaService) {

    data class CreateRelationRequest(
        val sourceCollectionId: Long,
        val targetCollectionId: Long,
        val sourceFieldIds: List<Long>,
        val targetFieldIds: List<Long>,
        val type: RelationType
    )

    @PostMapping
    fun createRelation(@RequestBody req: CreateRelationRequest): RelationMetadata {
        val meta = RelationMetadata(
            id = null,
            sourceCollectionId = req.sourceCollectionId,
            targetCollectionId = req.targetCollectionId,
            sourceFieldIds = req.sourceFieldIds,
            targetFieldIds = req.targetFieldIds,
            type = req.type
        )
        return schemaService.addRelation(meta)
    }
}
