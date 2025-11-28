// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
    // Use the version catalog from the root project
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "detekt-sample"

// Include the parent project as a composite build to reference the rules
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("io.nlopez.compose.rules:detekt")).using(project(":rules:detekt"))
    }
}
