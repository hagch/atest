package io.webcontify.collection.web

import io.webcontify.collection.schema.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/collections")
class CollectionController(private val schemaService: SchemaService) {

    data class CreateCollectionRequest(val name: String, val idGeneration: IdGeneration = IdGeneration.SEQUENCE)
    data class CreateFieldRequest(
        val name: String,
        val type: FieldType,
        val nullable: Boolean = true,
        val unique: Boolean = false,
        val defaultValue: String? = null,
        val enumValues: List<String>? = null
    )

    @PostMapping
    fun createCollection(@RequestBody req: CreateCollectionRequest) = schemaService.createCollection(req.name, req.idGeneration)

    @GetMapping
    fun listCollections(): List<CollectionMetadata> = schemaService.listCollections()

    @PostMapping("/{id}/fields")
    fun createField(
        @PathVariable id: Long,
        @RequestBody req: CreateFieldRequest
    ): FieldMetadata = schemaService.addField(
        id,
        req.name,
        req.type,
        req.nullable,
        req.unique,
        req.defaultValue,
        req.enumValues
    )
}
