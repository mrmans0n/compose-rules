// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.functional

import io.nlopez.compose.rules.functional.FunctionalTestUtils.composeRulesVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.createGradleRunner
import io.nlopez.compose.rules.functional.FunctionalTestUtils.ktlintVersion
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeFile
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeGradleProperties
import io.nlopez.compose.rules.functional.FunctionalTestUtils.writeSettings
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

/**
 * Functional tests for Kotlin version compatibility with compose-rules.
 *
 * These tests verify that compose-rules work correctly across different Kotlin compiler versions,
 * specifically testing that NO crashes (NoSuchMethodError) occur when processing code with context
 * receivers and context parameters. In the future we might include tests for other Kotlin features
 * if they cause issues
 */
class KotlinVersionCompatibilityFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @ParameterizedTest(name = "Kotlin {0} compatibility")
    @ValueSource(strings = ["2.0.21", "2.1.10", "2.2.21"])
    fun `compose rules process context receivers without crashing`(kotlinVersion: String) {
        setupKtlintProjectWithKotlinVersion(kotlinVersion)

        // Create a file with context receiver
        projectDir.writeFile(
            "src/main/kotlin/com/example/ContextReceiverExample.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.ColumnScope
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            context(ColumnScope)
            @Composable
            fun WithContextReceiver(modifier: Modifier = Modifier) {
                Text("Hi")
            }
            """,
        )

        // Create a file with context parameter
        projectDir.writeFile(
            "src/main/kotlin/com/example/ContextParameterExample.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.ColumnScope
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            context(scope: ColumnScope)
            @Composable
            fun WithContextParameter(modifier: Modifier = Modifier) {
                Text("Hello")
            }
            """,
        )

        // Run spotless check - should succeed (proper composables)
        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("spotlessKotlinCheck"),
        ).build()

        result.assertTaskSucceeded(":spotlessKotlinCheck")

        // Most importantly: No NoSuchMethodError should occur
        // This would indicate the fallback logic is working correctly across Kotlin versions
        result.assertOutputDoesNotContain("NoSuchMethodError")
        result.assertOutputDoesNotContain("contextReceiverList")
    }

    @ParameterizedTest(name = "Kotlin {0} - mixed context works")
    @ValueSource(strings = ["2.0.21", "2.1.10", "2.2.21"])
    fun `compose rules process mixed context without crashing`(kotlinVersion: String) {
        setupKtlintProjectWithKotlinVersion(kotlinVersion)

        // Test mixed context (both receiver and parameter in same declaration)
        projectDir.writeFile(
            "src/main/kotlin/com/example/MixedContext.kt",
            """
            package com.example

            import androidx.compose.foundation.layout.ColumnScope
            import androidx.compose.foundation.layout.RowScope
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            context(column: ColumnScope, RowScope)
            @Composable
            fun MixedContext(modifier: Modifier = Modifier) {
                Text("Mixed")
            }
            """,
        )

        // Should pass without crashing
        val result = createGradleRunner(
            projectDir = projectDir,
            arguments = listOf("spotlessKotlinCheck"),
        ).build()

        result.assertTaskSucceeded(":spotlessKotlinCheck")
        result.assertOutputDoesNotContain("NoSuchMethodError")
    }

    private fun setupKtlintProjectWithKotlinVersion(kotlinVersion: String) {
        projectDir.writeSettings("kotlin-compat-test-$kotlinVersion")
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
                compilerOptions {
                    // Enable context receivers/parameters
                    freeCompilerArgs.add("-Xcontext-receivers")
                }
            }
            """,
        )

        // Create .editorconfig - disable standard rules (because they don't like compose)
        projectDir.writeFile(
            ".editorconfig",
            """
            [*.{kt,kts}]
            ktlint_standard = disabled
            """,
        )
    }
}
