// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.RememberContentMissing
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RememberContentMissingCheckTest {

    private val rememberRuleAssertThat = assertThatRule { RememberContentMissingCheck() }

    @Test
    fun `passes when a non-remembered movableContentOf is used outside of a Composable`() {
        @Language("kotlin")
        val code =
            """
                val msof = movableContentOf { Text("X") }
            """.trimIndent()
        rememberRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when a non-remembered movableContentOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val something = movableContentOf { Text("X") }
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 21,
                detail = RememberContentMissing.MovableContentOfNotRemembered,
            ),
        )
    }

    @Test
    fun `errors when a non-remembered movableContentWithReceiverOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val something = movableContentWithReceiverOf { Text("X") }
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 21,
                detail = RememberContentMissing.MovableContentWithReceiverOfNotRemembered,
            ),
        )
    }
}
