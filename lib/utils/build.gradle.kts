plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    id("amt-lib.conventions")
}

dependencies {
    implementation(libs.logback)
    implementation(libs.hikari.cp)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.kotliquery)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation(project(":lib:testing"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
