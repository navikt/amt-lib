group = "no.nav.amt.lib"
version = "0.0.1"

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

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
