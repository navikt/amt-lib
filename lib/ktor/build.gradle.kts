plugins {
    id("amt-lib.conventions")
    id("java-library")
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":lib:viewmodels"))
    api(libs.jackson.kotlin)
    api(libs.jackson.jsr310)
    api(libs.caffeine)
    api(libs.logback)
    api(libs.ktor.client.core)
    testImplementation(libs.kotest.runner.junit5)
}

ktlint {
    version = "1.7.1"
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dkotest.framework.classpath.scanning.autoscan.disable=true")
}
