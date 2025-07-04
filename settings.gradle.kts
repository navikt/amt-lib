/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 * For more detailed information on multi-project builds, please refer to https://docs.gradle.org/8.7/userguide/multi_project_builds.html in the Gradle documentation.
 */

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "amt-lib"
include("lib")
include("lib:kafka")
findProject(":lib:kafka")?.name = "kafka"
include("lib:testing")
findProject(":lib:testing")?.name = "testing"
include("lib:utils")
findProject(":lib:utils")?.name = "utils"
include("lib:models")
findProject(":lib:models")?.name = "models"
include("lib:outbox")
findProject(":lib:outbox")?.name = "outbox"
