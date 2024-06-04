// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ParameterNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ParameterNamingCheckTest {

    private val ruleAssertThat = assertThatRule { ParameterNamingCheck() }

    @Test
    fun `errors when a param lambda is in the past tense`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(onClicked: () -> Boolean) { }
                @Composable
                fun A(onWrote: () -> Boolean) { }
                @Composable
                fun A(onPotatoed: Potato) { }
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(
                treatAsComposableLambda to "Potato",
            )
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 7,
                    detail = ParameterNaming.LambdaParametersInPresentTense,
                ),
                LintViolation(
                    line = 4,
                    col = 7,
                    detail = ParameterNaming.LambdaParametersInPresentTense,
                ),
                LintViolation(
                    line = 6,
                    col = 7,
                    detail = ParameterNaming.LambdaParametersInPresentTense,
                ),
            )
    }

    @Test
    fun `ignores lambdas that don't start with on`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(blehWrote: () -> Unit, mehChanged: () -> Unit, potatoed: Potato) {}
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(
                treatAsComposableLambda to "Potato",
            )
            .hasNoLintViolations()
    }

    @Test
    fun `passes when param lambdas are in present tense`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(
                    onClick: () -> Unit,
                    onValueChange: (Int) -> Unit,
                    onWrite: () -> Unit,
                    onPotato: Potato,
                    onEmbed: () -> Unit,
                    onFocusChanged: () -> Unit,
                    onValueChangeFinished: () -> Unit,
                ) {}
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(
                treatAsComposableLambda to "Potato",
            )
            .hasNoLintViolations()
    }
}
