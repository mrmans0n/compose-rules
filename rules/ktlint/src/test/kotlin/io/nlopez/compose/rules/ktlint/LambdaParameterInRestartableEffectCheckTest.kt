// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.LambdaParameterInRestartableEffect
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class LambdaParameterInRestartableEffectCheckTest {
    private val ruleAssertThat = KtLintAssertThat.assertThatRule { LambdaParameterInRestartableEffectCheck() }

    @Test
    fun `error out when detecting a lambda being used in an effect`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onClick: () -> Unit) {
                    LaunchedEffect(Unit) {
                        onClick()
                    }
                }
                @Composable
                fun Something(onClick: MyLambda) {
                    DisposableEffect(Unit) {
                        onClick()
                    }
                }
                fun interface MyLambda2 {
                    fun create()
                }
                @Composable
                fun Something(onClick: MyLambda2) {
                    LaunchedEffect(Unit) {
                        onClick()
                    }
                }
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(
                treatAsLambda to "MyLambda",
            )
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 15,
                    detail = LambdaParameterInRestartableEffect.LambdaUsedInRestartableEffect,
                ),
                LintViolation(
                    line = 8,
                    col = 15,
                    detail = LambdaParameterInRestartableEffect.LambdaUsedInRestartableEffect,
                ),
                LintViolation(
                    line = 17,
                    col = 15,
                    detail = LambdaParameterInRestartableEffect.LambdaUsedInRestartableEffect,
                ),
            )
    }

    @Test
    fun `passes when a lambda is properly handled before using it in an effect`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onClick: () -> Unit) {
                    val latestOnClick by rememberUpdatedState(onClick)
                    LaunchedEffect(Unit) {
                        latestOnClick()
                    }
                }
                @Composable
                fun Something(onClick: () -> Unit) {
                    DisposableEffect(onClick) {
                        onClick()
                    }
                }
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(
                treatAsLambda to "MyLambda",
            )
            .hasNoLintViolations()
    }
}
