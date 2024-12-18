plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    id("amt-lib.conventions")
}

dependencies {
    implementation(libs.jackson.jsr310)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation(libs.jackson.kotlin)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
