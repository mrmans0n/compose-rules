// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.functional

import io.nlopez.compose.rules.functional.FunctionalTestUtils.createGradleRunner
import io.nlopez.compose.rules.functional.FunctionalTestUtils.kotlinVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.ktlintCheckTask
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeFile
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeGradleProperties
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeSettings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Runs the published Compose ruleset with the ktlint CLI from Gradle. */
class KtlintFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `ModifierMissing is detected via the ktlint CLI`() {
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
            arguments = listOf("ktlintCheck"),
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
            arguments = listOf("ktlintCheck"),
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
            arguments = listOf("ktlintCheck"),
        ).build()

        result.assertTaskSucceeded(":ktlintCheck")
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

            $ktlintCheckTask

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
