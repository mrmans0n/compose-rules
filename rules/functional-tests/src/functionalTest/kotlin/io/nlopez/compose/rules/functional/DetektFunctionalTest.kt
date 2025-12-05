// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.functional

import io.nlopez.compose.rules.functional.FunctionalTestUtils.composeRulesVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.createGradleRunner
import io.nlopez.compose.rules.functional.FunctionalTestUtils.detektVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.kotlinVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeFile
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeGradleProperties
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeSettings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Functional tests for detekt integration with compose-rules.
 *
 * These tests create actual Gradle projects and run them to verify that:
 * 1. Rules are detected correctly via detekt
 * 2. Basic Gradle integration works (caching, up-to-date checks)
 */
class DetektFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `ModifierMissing is detected via detekt`() {
        setupDetektProject()

        projectDir.writeFile(
            "src/main/kotlin/com/example/Violations.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.Row
            import androidx.compose.runtime.Composable

            @Composable
            fun MissingModifierViolation() {
                Row {
                    // Missing modifier parameter
                }
            }
            """,
        )

        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("detekt"),
        ).buildAndFail()

        // Build should fail due to violations
        result.assertOutputContains("[ModifierMissing]")
    }

    @Test
    fun `multiple compose-rules violations are detected`() {
        setupDetektProject()

        projectDir.writeFile(
            "src/main/kotlin/com/example/MultipleViolations.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.Column
            import androidx.compose.foundation.layout.Row
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf

            @Composable
            fun MultipleViolations(items: ArrayList<String>) {
                val state = mutableStateOf("test")  // RememberMissing

                // Multiple emitters
                Column {
                    Text("First")
                }
                Row {
                    Text("Second")
                }
            }

            @Composable
            fun EventHandler(onClicked: () -> Unit) {  // ParameterNaming (should be onClick)
                // ...
            }
            """,
        )

        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("detekt"),
        ).buildAndFail()

        // Build should fail due to multiple violations
        result.assertOutputContains(
            "[ModifierMissing]",
            "[MutableParams]",
            "[RememberMissing]",
            "[MultipleEmitters]",
            "[ParameterNaming]",
        )
    }

    @Test
    fun `clean code passes detekt checks`() {
        setupDetektProject()

        projectDir.writeFile(
            "src/main/kotlin/com/example/CleanCode.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.Row
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.ui.Modifier

            @Composable
            fun ProperComposable(modifier: Modifier = Modifier) {
                val state = remember { mutableStateOf("test") }
                Row(modifier = modifier) {
                    // Properly structured composable
                }
            }
            """,
        )

        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("detekt"),
        ).build()

        result.assertTaskSucceeded(":detekt")
    }

    @Test
    fun `gradle integration - up-to-date checks work`() {
        setupDetektProject()

        projectDir.writeFile(
            "src/main/kotlin/com/example/Example.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.Row
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @Composable
            fun Example(modifier: Modifier = Modifier) {
                Row(modifier = modifier) { }
            }
            """,
        )

        // First build
        val firstResult = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("detekt"),
        ).build()

        firstResult.assertTaskSucceeded(":detekt")

        // Second build without changes should be up-to-date
        val secondResult = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("detekt"),
        ).build()

        secondResult.assertTaskUpToDate(":detekt")
    }

    @Test
    fun `detekt configuration can be customized`() {
        setupDetektProject(
            customDetektConfig = """
            naming:
              FunctionNaming:
                ignoreAnnotated:
                  - 'Composable'

            style:
              NewLineAtEndOfFile:
                active: false

            Compose:
              ModifierMissing:
                active: false
            """,
        )

        projectDir.writeFile(
            "src/main/kotlin/com/example/Violations.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.Row
            import androidx.compose.runtime.Composable

            @Composable
            fun MissingModifierViolation() {
                Row {
                    // This would normally violate, but we've disabled the rule
                }
            }
            """,
        )

        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("detekt"),
        ).build()

        // Should succeed since we disabled ModifierMissing
        result.assertTaskSucceeded(":detekt")
        result.assertOutputDoesNotContain("[ModifierMissing]")
    }

    // TODO: Add test for preview composable exclusion once configured properly
    // Preview composable handling may differ between ktlint and detekt configurations

    private fun setupDetektProject(customDetektConfig: String? = null) {
        projectDir.writeSettings("detekt-functional-test")
        projectDir.writeGradleProperties()

        projectDir.writeFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                kotlin("plugin.compose") version "$kotlinVersion"
                id("dev.detekt") version "$detektVersion"
            }

            repositories {
                mavenCentral()
                google()
                mavenLocal()
            }

            dependencies {
                implementation(platform("androidx.compose:compose-bom:2025.12.00"))
                implementation("androidx.compose.runtime:runtime")
                implementation("androidx.compose.foundation:foundation")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.ui:ui-tooling-preview")

                detektPlugins("io.nlopez.compose.rules:detekt:$composeRulesVersion")
            }

            detekt {
                config.setFrom(files("detekt.yml"))
                buildUponDefaultConfig = true
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }

            kotlin {
                jvmToolchain(21)
            }
            """,
        )

        val detektYml = if (customDetektConfig != null) {
            customDetektConfig
        } else {
            """
            naming:
              FunctionNaming:
                ignoreAnnotated:
                  - 'Composable'

            style:
              NewLineAtEndOfFile:
                active: false
              FunctionOnlyReturningConstant:
                active: false
              UnusedParameter:
                active: false

            Compose:
              ComposableParamOrder:
                active: false
              ModifierMissing:
                active: true
              MutableParams:
                active: true
              RememberMissing:
                active: true
              RememberContentMissing:
                active: true
              MultipleEmitters:
                active: true
              ComposableNaming:
                active: true
              ParameterNaming:
                active: true
            """
        }

        projectDir.writeFile("detekt.yml", detektYml)
    }
}
