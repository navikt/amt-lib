plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    id("amt-lib.conventions")
}

dependencies {
    api(libs.kotlinx.coroutines)
    implementation(libs.logback)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)
    implementation(libs.kotliquery)
    implementation(libs.postgresql)
    implementation(project(":lib:utils"))
    implementation(project(":lib:kafka"))

    testImplementation(project(":lib:testing"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
