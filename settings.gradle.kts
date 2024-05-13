// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("com.gradle.develocity") version "3.17.3"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "compose-rules"
include(":core-common")
include(":core-detekt")
include(":core-ktlint")
include(":rules:common")
include(":rules:detekt")
include(":rules:ktlint")
