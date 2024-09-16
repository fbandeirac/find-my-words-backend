package com.kivikood

import com.kivikood.plugins.configureFrameworks
import com.kivikood.plugins.configureRouting
import com.kivikood.plugins.configureSerialization
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*

val dotenv = dotenv()
val wordListSize: Int = dotenv["WORD_LIST_SIZE"]?.toInt() ?: 50
val elasticHost: String = dotenv["ELASTIC_HOST"] ?: "localhost"
val elasticPort: Int = dotenv["ELASTIC_PORT"]?.toInt() ?: 9200
val elasticScheme: String = dotenv["ELASTIC_SCHEME"] ?: "http"
val addWordToken: String = dotenv["ADD_WORD_TOKEN"] ?: "default_token"

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureFrameworks()
    configureRouting()
}
