package com.kivikood.plugins

import com.kivikood.addWordToken
import com.kivikood.domain.SearchCriteria
import com.kivikood.services.ElasticsearchService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*



fun Application.configureRouting(esService: ElasticsearchService = ElasticsearchService()) {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        post("/search") {
            val request = call.receive<Map<String, String>>()
            val language = request["language"] ?: return@post call.respondText(
                "Missing language",
                status = HttpStatusCode.BadRequest
            )
            val startingIndex = request["startingIndex"]?.toIntOrNull()

            val words = esService.listWords(language, startingIndex)

            if (words.isNotEmpty()) {
                call.respond(words)
            } else {
                call.respondText("No results found for language $language")
            }
        }

        // Endpoint to add a word to Elasticsearch
        post("/add-word") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            if (token == null || token != addWordToken) {
                return@post call.respondText("Invalid or missing token", status = HttpStatusCode.Unauthorized)
            }
            // Receive the word and language from the request body
            val request = call.receive<Map<String, String>>()
            val word =
                request["word"] ?: return@post call.respondText("Missing word", status = HttpStatusCode.BadRequest)
            val language = request["language"] ?: return@post call.respondText(
                "Missing language",
                status = HttpStatusCode.BadRequest
            )

            try {
                // Add the word to Elasticsearch
                val response = esService.addWord(word, language)

                // Respond with the document ID
                call.respondText("Word added with ID: ${response.id()}")
            } catch (e: Exception) {
                // Handle case where the word already exists for the language
                if (e.message?.contains("version_conflict_engine_exception") == true) {
                    call.respondText("Word already exists in the language $language", status = HttpStatusCode.Conflict)
                } else {
                    call.respondText("Error adding word: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }
        }

        // Search with criteria
        post("/search-with-criteria") {

            // Receive search criteria from request body
            val searchCriteria = call.receive<SearchCriteria>()

            val language = searchCriteria.language ?: return@post call.respondText(
                "Missing language",
                status = HttpStatusCode.BadRequest
            )

            // Perform the search operation with criteria
            val words = esService.searchWithCriteria(searchCriteria)

            if (words.isNotEmpty()) {
                call.respond(words)
            } else {
                call.respondText("No results found for language $language")
            }
        }

        // Bulk insert words
        post("/bulk/{language}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            if (token == null || token != addWordToken) {
                return@post call.respondText("Invalid or missing token", status = HttpStatusCode.Unauthorized)
            }

            val language = call.parameters["language"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "Language is missing"
            )
            val multipart = call.receiveMultipart()

            var words = listOf<String>()
            // Read the file from the multipart form data
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val fileBytes = part.streamProvider().readBytes()
                    val fileContent = String(fileBytes)
                    words = fileContent.lines().filter { it.isNotBlank() } // Get list of words
                    part.dispose()
                }
            }

            if (words.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "No words found in file")
                return@post
            }

            val responseMessage = esService.bulkAddWords(words, language)
            call.respond(HttpStatusCode.OK, responseMessage)

        }
    }
}
