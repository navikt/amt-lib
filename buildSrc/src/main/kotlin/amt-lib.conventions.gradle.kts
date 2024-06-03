import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.net.URI

group = "no.nav.amt.lib"

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

publishing {
    publications {
        create<MavenPublication>("amt-lib") {
            from(components["kotlin"])
        }
    }

    repositories {
        maven {
            url = URI("https://maven.pkg.github.com/navikt/amt-lib")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
