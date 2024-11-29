// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "3.18.2"
    id("org.jetbrains.intellij.platform.settings") version "2.1.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform { defaultRepositories() }
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
    ":rules:detekt",
    ":rules:intellij",
    ":rules:ktlint",
)
