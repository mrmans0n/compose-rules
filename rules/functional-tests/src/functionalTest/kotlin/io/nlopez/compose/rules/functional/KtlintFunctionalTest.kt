// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.functional

import io.nlopez.compose.rules.functional.FunctionalTestUtils.composeRulesVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.createGradleRunner
import io.nlopez.compose.rules.functional.FunctionalTestUtils.kotlinVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.ktlintVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeFile
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeGradleProperties
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeSettings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Functional tests for ktlint integration with compose-rules.
 *
 * These tests create actual Gradle projects and run them to verify that:
 * 1. Rules are detected correctly via spotless/ktlint
 * 2. Basic Gradle integration works (caching, up-to-date checks)
 */
class KtlintFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `ModifierMissing is detected via ktlint spotless`() {
        setupKtlintProject()

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
            arguments = listOf("spotlessKotlinCheck"),
        ).buildAndFail()

        // Build should fail due to violations
        result.assertOutputContains("compose:modifier-missing-check")
    }

    @Test
    fun `multiple rules are detected in separate files`() {
        setupKtlintProject()

        // ModifierMissing violation
        projectDir.writeFile(
            "src/main/kotlin/com/example/ModifierViolation.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.Row
            import androidx.compose.runtime.Composable

            @Composable
            fun MissingModifier() {
                Row { }
            }
            """,
        )

        // MutableParams violation
        projectDir.writeFile(
            "src/main/kotlin/com/example/MutableParamsViolation.kt",
            """
            package com.example

            import androidx.compose.runtime.Composable

            @Composable
            fun MutableParamViolation(items: ArrayList<String>) {
                // Using mutable ArrayList as parameter
            }
            """,
        )

        // Parameter naming violation
        projectDir.writeFile(
            "src/main/kotlin/com/example/ParameterNamingViolation.kt",
            """
            package com.example

            import androidx.compose.runtime.Composable

            @Composable
            fun EventHandler(onClicked: () -> Unit) {
                // Should be onClick, not onClicked
            }
            """,
        )

        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("spotlessKotlinCheck"),
        ).buildAndFail()

        // Build should fail due to multiple violations
        result.assertOutputContains(
            "compose:modifier-missing-check",
            "compose:mutable-params-check",
            "compose:parameter-naming",
        )
    }

    @Test
    fun `clean code passes ktlint checks`() {
        setupKtlintProject()

        projectDir.writeFile(
            "src/main/kotlin/com/example/CleanCode.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.Row
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @Composable
            fun ProperComposable(modifier: Modifier = Modifier) {
                Row(modifier = modifier) {
                    // Properly structured composable
                }
            }
            """,
        )

        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("spotlessKotlinCheck"),
        ).build()

        result.assertTaskSucceeded(":spotlessKotlinCheck")
    }

    @Test
    fun `gradle integration - up-to-date checks work`() {
        setupKtlintProject()

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
            arguments = listOf("spotlessKotlinCheck"),
        ).build()

        firstResult.assertTaskSucceeded(":spotlessKotlinCheck")

        // Second build without changes should be up-to-date
        val secondResult = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("spotlessKotlinCheck"),
        ).build()

        secondResult.assertTaskUpToDate(":spotlessKotlinCheck")
    }

    // TODO: Add test for preview composable exclusion once configured properly
    // Preview composable handling may differ between ktlint and detekt configurations

    private fun setupKtlintProject() {
        projectDir.writeSettings("ktlint-functional-test")
        projectDir.writeGradleProperties()

        projectDir.writeFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                kotlin("plugin.compose") version "$kotlinVersion"
                id("com.diffplug.spotless") version "8.1.0"
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
            }

            spotless {
                kotlin {
                    target("**/*.kt")
                    ktlint("$ktlintVersion")
                        .setEditorConfigPath("${"$"}{project.rootDir}/.editorconfig")
                        .customRuleSets(
                            listOf(
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
            """,
        )

        projectDir.writeFile(
            ".editorconfig",
            """
            [*.{kt,kts}]
            ktlint_standard = disabled
            """,
        )
    }
}
