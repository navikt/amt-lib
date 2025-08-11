plugins {
    id("amt-lib.conventions")
    id("java-library")
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":lib:models"))
    api(project(":lib:utils"))
    api(libs.jackson.kotlin)
    api(libs.caffeine)
    api(libs.logback)
    api(libs.ktor.client.core)

    testImplementation(project(":lib:testing"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)

    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.serialization.jackson)
}

ktlint {
    version = "1.7.1"
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dkotest.framework.classpath.scanning.autoscan.disable=true")
}
