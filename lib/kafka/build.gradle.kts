plugins {
    alias(libs.plugins.serialization)
    id("amt-lib.conventions")
}

dependencies {
    implementation("at.yawk.lz4:lz4-java:1.10.1") // fjernes ved neste release av org.apache.kafka:kafka-clients
    api(libs.kafka.clients)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.logback)
    implementation(project(":lib:utils"))

    testImplementation(project(":lib:testing"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
}
