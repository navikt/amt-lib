plugins {
    alias(libs.plugins.serialization)
    id("amt-lib.conventions")
}

dependencies {
    implementation("at.yawk.lz4:lz4-java:1.10.3") // fjernes ved neste release av org.apache.kafka:kafka-clients
    implementation(project(":lib:models"))
    implementation(project(":lib:utils"))
    implementation(project(":lib:outbox"))

    implementation(libs.kafka.clients)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback)
    implementation(libs.kotest.assertions.core)
    implementation(libs.kotest.assertions.json)
    implementation(libs.testcontainers.kafka)
    implementation(libs.testcontainers.postgresql)
    implementation(libs.hikari.cp)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.kotliquery)
    api(libs.caffeine)
    api(libs.junit.jupiter.params)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
}
