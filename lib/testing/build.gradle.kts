plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    id("amt-lib.conventions")
}

dependencies {
    implementation(project(":lib:models"))

    implementation(libs.kafka.clients)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.logback)
    implementation(libs.kotest.assertions.core)
    implementation(libs.kotest.assertions.json)
    implementation(libs.testcontainers)
    implementation(libs.testcontainers.kafka)
    implementation(libs.testcontainers.postgresql)
    implementation(libs.hikari.cp)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.kotliquery)
    implementation(project(":lib:utils"))

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
