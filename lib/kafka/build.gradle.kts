plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    id("amt-lib.conventions")
}

dependencies {
    api(libs.kafka.clients)
    api(libs.kotlinx.coroutines)
    implementation(libs.logback)
    implementation(project(":lib:utils"))

    testImplementation(project(":lib:testing"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
