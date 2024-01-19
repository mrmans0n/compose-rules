// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.LambdaParameterInRestartableEffect
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class LambdaParameterInRestartableEffectCheckTest {

    private val testConfig = TestConfig(
        "treatAsLambda" to listOf("MyLambda"),
    )
    private val rule = LambdaParameterInRestartableEffectCheck(testConfig)

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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 15),
                SourceLocation(8, 15),
                SourceLocation(17, 15),
            )
        for (error in errors) {
            assertThat(error).hasMessage(LambdaParameterInRestartableEffect.LambdaUsedInRestartableEffect)
        }
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
