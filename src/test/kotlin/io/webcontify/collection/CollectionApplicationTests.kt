package io.webcontify.collection

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CollectionApplicationTests {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun register(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `create collection and item`() {
        val createCollection = mapOf("name" to "notes")
        val collection = restTemplate.postForObject("/collections", createCollection, Map::class.java)
        assertNotNull(collection!!["id"])

        val collectionId = collection["id"].toString().toLong()
        restTemplate.postForObject("/collections/$collectionId/fields", mapOf("name" to "title", "type" to "TEXT", "nullable" to false), Map::class.java)

        val itemId = restTemplate.postForObject("/collections/$collectionId/items", mapOf("title" to "hello"), Long::class.java)
        assertNotNull(itemId)
        val item = restTemplate.getForObject("/collections/$collectionId/items/$itemId", Map::class.java)
        assertNotNull(item)
    }
}
