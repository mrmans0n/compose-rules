// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.spotless)
}

// Get compose-rules version from root project's gradle.properties
val composeRulesVersion: String by lazy {
    val props = Properties()
    file("../../gradle.properties").inputStream().use { props.load(it) }
    props.getProperty("VERSION_NAME")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("${project.rootDir}/.editorconfig")
            .customRuleSets(
                listOf(
                    // Add compose-rules ktlint ruleset
                    "io.nlopez.compose.rules:ktlint:$composeRulesVersion"
                )
            )
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}
