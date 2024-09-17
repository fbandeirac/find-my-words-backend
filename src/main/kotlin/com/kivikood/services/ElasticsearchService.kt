package com.kivikood.services

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.OpType
import co.elastic.clients.elasticsearch._types.Script
import co.elastic.clients.elasticsearch._types.query_dsl.*
import co.elastic.clients.elasticsearch.core.*
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.kivikood.elasticHost
import com.kivikood.elasticPort
import com.kivikood.elasticScheme
import com.kivikood.domain.SearchCriteria
import com.kivikood.domain.WordDocument
import com.kivikood.wordListSize
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient

class ElasticsearchService(hostname: String = elasticHost, port: Int = elasticPort, scheme: String = elasticScheme) {

    // Create the low-level client
    private val restClient: RestClient = RestClient.builder(
        HttpHost(hostname, port, scheme)
    ).build()

    // Create the transport with a Jackson mapper
    private val transport = RestClientTransport(restClient, JacksonJsonpMapper())

    // Create the Elasticsearch client
    val client = ElasticsearchClient(transport)

    // Method to list words
    fun listWords(language: String, startingIndex: Int? = null): List<String> {
        val startingIndex = (startingIndex ?: 0) * wordListSize
        val searchRequest = SearchRequest.Builder().index("words")
            .query(
                BoolQuery.Builder()
                    .must(
                        MatchQuery.Builder().field("language").query(language).build()._toQuery()
                    )
                    .build()._toQuery()
            )
            .from(startingIndex)
            .size(wordListSize)
            .build()

        val response: SearchResponse<WordDocument> = client.search(searchRequest, WordDocument::class.java)

        // Return the list of words, ensuring no extra quotes are added
        return response.hits().hits().map { it.source()?.word?.trim('"') ?: "" }.toList()
    }

    fun searchWithCriteria(criteria: SearchCriteria): List<String> {
        val boolQuery = BoolQuery.Builder()

        // Add language as a filter
        boolQuery.must(
            MatchQuery.Builder().field("language").query(criteria.language).build()._toQuery()
        )

        // Length range (minLength, maxLength)
        if (criteria.minLength != null || criteria.maxLength != null) {
            val scriptContent = """
            doc['word.keyword'].value.length() >= ${criteria.minLength ?: 0} && 
            doc['word.keyword'].value.length() <= ${criteria.maxLength ?: Int.MAX_VALUE}
        """.trimIndent()

            boolQuery.must(
                ScriptQuery.Builder().script(
                    Script.of {
                        it.inline { inlineScript ->
                            inlineScript.source(scriptContent)
                                .lang("painless")
                        }
                    }
                ).build()._toQuery()
            )
        }

        // Starting with a specific character
        if (criteria.startWith != null) {
            boolQuery.must(
                PrefixQuery.Builder().field("word.keyword").value(criteria.startWith.toString()).build()._toQuery()
            )
        }

        // Ending with a specific character
        if (criteria.endWith != null) {
            boolQuery.must(
                WildcardQuery.Builder().field("word.keyword").value("*${criteria.endWith}").build()._toQuery()
            )
        }

        // Obligatory characters
        criteria.obligatoryCharacters?.forEach { char ->
            boolQuery.must(
                WildcardQuery.Builder().field("word.keyword").value("*$char*").build()._toQuery()
            )
        }

        // Character set (ensure word only contains specific characters)
        if (criteria.characterSet != null) {
            val regex = criteria.characterSet.joinToString("") { "\\Q$it\\E" } // Escape regex special characters
            boolQuery.must(
                RegexpQuery.Builder().field("word.keyword").value("[$regex]+").build()._toQuery()
            )
        }

        val startingIndex = (criteria.startingIndex ?: 0) * wordListSize

        // Build the search request
        val searchRequest = SearchRequest.Builder().index("words")
            .query(boolQuery.build()._toQuery()).from(startingIndex).size(wordListSize).build()

        // Execute the search
        val response: SearchResponse<WordDocument> = client.search(searchRequest, WordDocument::class.java)

        // Return the list of words, ensuring they are unique
        return response.hits().hits().map { it.source()?.word?.trim('"') ?: "" }.toList()
    }

    fun addWord(word: String, language: String): IndexResponse {
        // Use a composite key (language + word) to enforce uniqueness per language
        val wordDocument = WordDocument("$language-$word", word, language)

        return try {
            client.index<WordDocument> { request ->
                request.index("words")
                    .id(wordDocument.id)
                    .document(wordDocument)
                    .opType(OpType.Create)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun bulkAddWords(words: List<String>, language: String): String {
        val bulkRequest = BulkRequest.Builder().apply {
            words.forEach { word ->
                val wordDocument = WordDocument("$language-$word", word, language)
                this.operations { op ->
                    op.create { createOp ->
                        createOp.index("words")
                            .id(wordDocument.id)
                            .document(wordDocument)
                    }
                }
            }
        }.build()

        val bulkResponse: BulkResponse = client.bulk(bulkRequest)

        // Handle response and find errors (like duplicate indexes)
        val duplicateErrors = bulkResponse.items().filter { it.error() != null }
        val successCount = bulkResponse.items().size - duplicateErrors.size

        val responseMessage = buildString {
            append("Inserted $successCount words successfully.\n")
            if (duplicateErrors.isNotEmpty()) {
                append("Failed to insert ${duplicateErrors.size} words due to duplicate IDs:\n")
                duplicateErrors.forEach { item ->
                    append(" - Word ID: ${item.id()}, Error: ${item.error()?.reason()}\n")
                }
            }
        }
        return responseMessage
    }

}
