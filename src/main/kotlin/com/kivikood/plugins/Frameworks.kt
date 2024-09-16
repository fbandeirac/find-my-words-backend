package com.kivikood.plugins

import com.kivikood.services.ElasticsearchService
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()  // Enable logging for Koin
        modules(module {
            single { ElasticsearchService() }  // Register ElasticsearchService as a singleton
        })
    }
}
