// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.LambdaParameterEventTrailing
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class LambdaParameterEventTrailingCheckTest {
    private val ruleAssertThat = KtLintAssertThat.assertThatRule { LambdaParameterEventTrailingCheck() }

    @Test
    fun `error out when detecting a lambda being as trailing`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(modifier: Modifier = Modifier, onClick: () -> Unit) {
                    Text("Hello")
                }
            """.trimIndent()
        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 46,
                detail = LambdaParameterEventTrailing.EventLambdaIsTrailingLambda,
            ),
        )
    }

    @Test
    fun `passes when a lambda is required`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onClick: () -> Unit, modifier: Modifier = Modifier) {
                    Text("Hello")
                }
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when a lambda is composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(modifier: Modifier = Modifier, on: @Composable () -> Unit) {
                    Text("Hello")
                }
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when the function doesnt emit content`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun something(modifier: Modifier = Modifier, onClick: () -> Unit) {}
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }
}
