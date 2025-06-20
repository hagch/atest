package io.webcontify.collection.web

import io.webcontify.collection.item.ItemService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/collections/{collectionId}/items")
class ItemController(private val itemService: ItemService) {

    @PostMapping
    fun create(
        @PathVariable collectionId: Long,
        @RequestBody body: Map<String, Any?>
    ): Long = itemService.createItem(collectionId, body)

    @GetMapping("/{id}")
    fun get(@PathVariable collectionId: Long, @PathVariable id: Long) = itemService.findItem(collectionId, id)

    @PutMapping("/{id}")
    fun update(
        @PathVariable collectionId: Long,
        @PathVariable id: Long,
        @RequestBody body: Map<String, Any?>
    ) = itemService.updateItem(collectionId, id, body)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable collectionId: Long, @PathVariable id: Long) = itemService.deleteItem(collectionId, id)

    @GetMapping
    fun search(
        @PathVariable collectionId: Long,
        @RequestParam params: Map<String, String>
    ) = itemService.search(collectionId, params)
}
