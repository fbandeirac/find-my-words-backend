package com.kivikood.routing

import co.elastic.clients.elasticsearch.core.IndexResponse
import com.kivikood.plugins.configureRouting
import com.kivikood.services.ElasticsearchService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.test.KoinTest
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddWordTests : KoinTest {

    companion object {
        private const val ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:7.17.10"
    }

    private lateinit var esContainer: ElasticsearchContainer
    private lateinit var esService: ElasticsearchService

    @BeforeAll
    fun setUp() {
        // Start the Elasticsearch container before all tests
        esContainer = ElasticsearchContainer(DockerImageName.parse(ELASTICSEARCH_IMAGE)).apply {
            start()
        }

        // Mock the Elasticsearch service and connect it to the Testcontainer instance
        esService = ElasticsearchService(
            esContainer.httpHostAddress,
            esContainer.firstMappedPort,
            "http"
        )
    }

    @AfterAll
    fun tearDown() {
        // Stop and clean up the container after all tests
        esContainer.stop()
    }

    @Test
    fun `test add word success`() = testApplication {
        val indexResponse = mockk<IndexResponse>()
        every { indexResponse.shards().successful() } returns 1  // Assuming success if shards are successful

        coEvery { esService.addWord(any(), any()) } returns indexResponse

        application {
            configureRouting(esService)
        }

        client.post("/add-word") {
            contentType(ContentType.Application.Json)
            setBody("""{"word":"abacaxi","language":"pt-br"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Word added successfully", bodyAsText())
        }

        // Verify that Elasticsearch service was called
        coVerify { esService.addWord("abacaxi", "pt-br") }
    }

    @Test
    fun `test add duplicate word`() = testApplication {
        val indexResponse = mockk<IndexResponse>()
        every { indexResponse.shards().failed() } returns 2

        coEvery { esService.addWord(any(), any()) } returns indexResponse

        application {
            configureRouting(esService)
        }

        client.post("/add-word") {
            contentType(ContentType.Application.Json)
            setBody("""{"word":"abacaxi","language":"pt-br"}""")
        }.apply {
            assertEquals(HttpStatusCode.Conflict, status)
            assertEquals("Word already exists", bodyAsText())
        }

        coVerify { esService.addWord("abacaxi", "pt-br") }
    }

    @Test
    fun `test add word invalid json`() = testApplication {
        application {
            configureRouting(esService)
        }

        client.post("/add-word") {
            contentType(ContentType.Application.Json)
            setBody("""{"invalid":"json"}""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("Invalid request body", bodyAsText())
        }

        coVerify(exactly = 0) { esService.addWord(any(), any()) }
    }

    @Test
    fun `test add word when Elasticsearch service fails`() = testApplication {
        coEvery { esService.addWord(any(), any()) } throws Exception("Elasticsearch failure")

        application {
            configureRouting(esService)
        }

        client.post("/add-word") {
            contentType(ContentType.Application.Json)
            setBody("""{"word":"abacaxi","language":"pt-br"}""")
        }.apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertEquals("Failed to add word", bodyAsText())
        }

        coVerify { esService.addWord("abacaxi", "pt-br") }
    }

    @Test
    fun `test add word missing language field`() = testApplication {
        application {
            configureRouting(esService)
        }

        client.post("/add-word") {
            contentType(ContentType.Application.Json)
            setBody("""{"word":"abacaxi"}""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("Missing language", bodyAsText())
        }

        coVerify(exactly = 0) { esService.addWord(any(), any()) }
    }

    @Test
    fun `test add word missing word field`() = testApplication {
        application {
            configureRouting(esService)
        }

        client.post("/add-word") {
            contentType(ContentType.Application.Json)
            setBody("""{"language":"pt-br"}""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("Missing word", bodyAsText())
        }

        coVerify(exactly = 0) { esService.addWord(any(), any()) }
    }
}
