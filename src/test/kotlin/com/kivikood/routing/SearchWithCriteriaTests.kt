package com.kivikood.routing

import com.kivikood.services.ElasticsearchService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchWithCriteriaTests {

    // Create a mocked Elasticsearch service for testing
    private val mockElasticsearchService = mockk<ElasticsearchService>()

    @Test
    fun `test search with criteria returns word`() = testApplication {
        val client = createClient {
            this@testApplication.install(ContentNegotiation) {
                json()
            }
        }

        // Add word to the dictionary
        client.post("/add-word") {
            contentType(ContentType.Application.Json)
            setBody("""{ "word": "abacaxi", "language": "pt-br" }""")
        }

        // Search for the word
        val searchResponse = client.post("/search-with-criteria") {
            contentType(ContentType.Application.Json)
            setBody("""{ "word": "abacaxi", "language": "pt-br" }""")
        }

        // Assert that the word is found
        assertEquals(HttpStatusCode.OK, searchResponse.status)
        assertTrue(searchResponse.bodyAsText().contains("abacaxi"))
    }


    @Test
    fun `test search with missing language`() = testApplication {
        // Call the search-with-criteria route with missing language
        val response = client.post("/search-with-criteria") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "minLength": 3,
                  "maxLength": 7,
                  "startWith": "a"
                }
            """.trimIndent()
            )
        }

        // Verify that the response returns BadRequest when language is missing
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Missing or empty language field"))
    }

    @Test
    fun `test search with empty language`() = testApplication {
        // Call the search-with-criteria route with an empty language field
        val response = client.post("/search-with-criteria") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "minLength": 3,
                  "maxLength": 7,
                  "startWith": "a",
                  "language": ""
                }
            """.trimIndent()
            )
        }

        // Verify that the response returns BadRequest when language is empty
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Missing or empty language field"))
    }

    @Test
    fun `test search with no results`() = testApplication {
        // Mocking the searchWithCriteria function to return an empty list (no results)
        every { mockElasticsearchService.searchWithCriteria(any()) } returns emptyList()

        // Call the search-with-criteria route with valid data
        val response = client.post("/search-with-criteria") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "minLength": 6,
                  "maxLength": 8,
                  "startWith": "a",
                  "language": "pt-br"
                }
            """.trimIndent()
            )
        }

        // Verify that the response returns "No results found"
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("No results found"))
    }
}
