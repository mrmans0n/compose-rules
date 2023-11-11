// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.PreviewPublic
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class PreviewPublicCheckTest {

    private val ruleAssertThat = assertThatRule { PreviewPublicCheck() }

    @Test
    fun `passes for non-preview public composables`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable() { }
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors for preview public composables`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun MyComposable() { }
            @CombinedPreviews
            @Composable
            fun MyComposable() { }
            """.trimIndent()
        ruleAssertThat(code).hasLintViolations(
            LintViolation(
                line = 3,
                col = 5,
                detail = PreviewPublic.ComposablesPreviewShouldNotBePublic,
            ),
            LintViolation(
                line = 6,
                col = 5,
                detail = PreviewPublic.ComposablesPreviewShouldNotBePublic,
            ),
        )
    }

    @Test
    fun `passes when a non-public preview composable uses preview params`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            private fun MyComposable(user: User) {
            }
            @CombinedPreviews
            @Composable
            internal fun MyComposable(user: User) {
            }
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `autofix makes private the public preview`() {
        @Language("kotlin")
        val badCode = """
            @Preview
            @Composable
            fun MyComposable(user: User) {
            }
            @CombinedPreviews
            @Composable
            fun MyComposable(user: User) {
            }
        """.trimIndent()

        @Language("kotlin")
        val expectedCode = """
            @Preview
            @Composable
            private fun MyComposable(user: User) {
            }
            @CombinedPreviews
            @Composable
            private fun MyComposable(user: User) {
            }
        """.trimIndent()
        ruleAssertThat(badCode).isFormattedAs(expectedCode)
    }
}
