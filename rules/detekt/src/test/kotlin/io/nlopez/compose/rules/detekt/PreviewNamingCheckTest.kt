// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.PreviewNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class PreviewNamingCheckTest {

    private val rule = PreviewNamingCheck(Config.empty)

    @Test
    fun `fails (suffix)`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun A() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(3, 5),
        )
        for (error in errors) {
            assertThat(error).hasMessage(PreviewNaming.PreviewDoesNotEndWithPreview)
        }
    }

    @Test
    fun `fails (prefix)`() {
        val rule = PreviewNamingCheck(TestConfig("previewNamingStrategy" to "prefix"))

        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun A() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(3, 5),
        )
        for (error in errors) {
            assertThat(error).hasMessage(PreviewNaming.PreviewDoesNotStartWithPreview)
        }
    }

    @Test
    fun `fails (anywhere)`() {
        val rule = PreviewNamingCheck(TestConfig("previewNamingStrategy" to "anywhere"))

        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun A() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(3, 5),
        )
        for (error in errors) {
            assertThat(error).hasMessage(PreviewNaming.PreviewDoesNotContainPreview)
        }
    }

    @Test
    fun `passes (suffix)`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun APreview() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes (prefix)`() {
        val rule = PreviewNamingCheck(TestConfig("previewNamingStrategy" to "prefix"))

        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun PreviewA() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes (anywhere)`() {
        val rule = PreviewNamingCheck(TestConfig("previewNamingStrategy" to "anywhere"))

        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun BananaPreviewPotato() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
