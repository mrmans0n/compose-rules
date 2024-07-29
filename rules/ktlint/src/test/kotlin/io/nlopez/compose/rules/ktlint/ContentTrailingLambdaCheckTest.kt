// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ContentTrailingLambda
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ContentTrailingLambdaCheckTest {

    private val ruleAssertThat = assertThatRule { ContentTrailingLambdaCheck() }

    @Test
    fun `errors when there is a content parameter not being last`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: @Composable () -> Unit, text: String) {}
            """.trimIndent()

        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 7,
                detail = ContentTrailingLambda.ContentShouldBeTrailingLambda,
            ),
        )
    }

    @Test
    fun `errors when there is an inferred content parameter not being last`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: Potato, text: String) {}
                @Composable
                fun A(content: Apple, text: String) {}
                @Composable
                fun A(content: Banana, text: String) {}

                typealias Apple = @Composable () -> Unit

                fun interface Banana {
                    @Composable fun Content()
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(treatAsComposableLambda to "Potato")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 7,
                    detail = ContentTrailingLambda.ContentShouldBeTrailingLambda,
                ),
                LintViolation(
                    line = 4,
                    col = 7,
                    detail = ContentTrailingLambda.ContentShouldBeTrailingLambda,
                ),
                LintViolation(
                    line = 6,
                    col = 7,
                    detail = ContentTrailingLambda.ContentShouldBeTrailingLambda,
                ),
            )
    }

    @Test
    fun `passes when content is the last parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(text: String, content: @Composable () -> Unit) {}
                @Composable
                fun A(content: @Composable Plum) {}
                @Composable
                fun A(text: String, content: Potato) {}
                @Composable
                fun A(text: String, content: Banana) {}
                @Composable
                fun A(text: String, content: Apple) {}

                typealias Apple = @Composable () -> Unit

                fun interface Banana {
                    @Composable fun Content()
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(treatAsComposableLambda to "Potato", treatAsLambda to "Plum")
            .hasNoLintViolations()
    }
}
