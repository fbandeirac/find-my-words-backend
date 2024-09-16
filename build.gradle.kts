val kotlin_version: String by project
val logback_version: String by project
val koin_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
}

group = "com.kivikood"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor dependencies
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")

    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")

    // Koin for Dependency Injection
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // Elasticsearch Java Client
    implementation("co.elastic.clients:elasticsearch-java:8.9.0")

    // Jackson dependencies for Elasticsearch
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.testcontainers:testcontainers:1.18.3")
    testImplementation("org.testcontainers:elasticsearch:1.18.3")
    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("io.insert-koin:koin-test:$koin_version")
}

