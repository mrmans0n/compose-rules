// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.PreviewNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class PreviewNamingCheckTest {

    private val ruleAssertThat = assertThatRule { PreviewNamingCheck() }

    @Test
    fun `fails (suffix)`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun A() { }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(
                composePreviewNamingEnabled to true,
                composePreviewNamingStrategy to "suffix",
            )
            .hasLintViolations(
                LintViolation(
                    line = 3,
                    col = 5,
                    detail = PreviewNaming.PreviewDoesNotEndWithPreview,
                ),
            )
            .isFormattedAs(
                """
                @Preview
                @Composable
                fun APreview() { }
                """.trimIndent(),
            )
    }

    @Test
    fun `fails (prefix)`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun A() { }
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(
                composePreviewNamingEnabled to true,
                composePreviewNamingStrategy to "prefix",
            )
            .hasLintViolations(
                LintViolation(
                    line = 3,
                    col = 5,
                    detail = PreviewNaming.PreviewDoesNotStartWithPreview,
                ),
            )
            .isFormattedAs(
                """
                @Preview
                @Composable
                fun PreviewA() { }
                """.trimIndent(),
            )
    }

    @Test
    fun `fails (anywhere)`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun A() { }
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(
                composePreviewNamingEnabled to true,
                composePreviewNamingStrategy to "anywhere",
            )
            .hasLintViolations(
                LintViolation(
                    line = 3,
                    col = 5,
                    detail = PreviewNaming.PreviewDoesNotContainPreview,
                ),
            )
            .isFormattedAs(
                """
                @Preview
                @Composable
                fun APreview() { }
                """.trimIndent(),
            )
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
        ruleAssertThat(code)
            .withEditorConfigOverride(
                composePreviewNamingEnabled to true,
                composePreviewNamingStrategy to "suffix",
            )
            .hasNoLintViolations()
    }

    @Test
    fun `passes (prefix)`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun PreviewA() { }
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(
                composePreviewNamingEnabled to true,
                composePreviewNamingStrategy to "prefix",
            )
            .hasNoLintViolations()
    }

    @Test
    fun `passes (anywhere)`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun BananaPreviewPotato() { }
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(
                composePreviewNamingEnabled to true,
                composePreviewNamingStrategy to "anywhere",
            )
            .hasNoLintViolations()
    }
}
