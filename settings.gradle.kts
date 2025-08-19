// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("com.gradle.develocity") version "4.1.1"
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
include(
    ":rules:common",
    ":rules:common-detekt",
    ":rules:common-ktlint",
    ":rules:detekt",
    ":rules:ktlint",
)
