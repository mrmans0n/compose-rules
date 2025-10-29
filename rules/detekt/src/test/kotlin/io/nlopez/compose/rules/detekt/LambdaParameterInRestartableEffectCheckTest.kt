// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.SourceLocation
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.nlopez.compose.rules.LambdaParameterInRestartableEffect
import io.nlopez.compose.rules.detekt.assertThat
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

    @Test
    fun `passes when the lambda parameter is shadowed before using it in an effect`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onClick: () -> Unit) {
                    val onClick by rememberUpdatedState(onClick)
                    LaunchedEffect(Unit) {
                        onClick()
                    }
                }
                @Composable
                fun Something(onClick: () -> Unit) {
                    val (onClick, bleh) = something()
                    LaunchedEffect(Unit) {
                        onClick()
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when the a dot reference uses the same name as the lambda parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onClick: () -> Unit) {
                    LaunchedEffect(Unit) {
                        viewModel.onClick()
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `error out when detecting a lambda named onDispose used in a non-DisposableEffect`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onDispose: () -> Unit) {
                    LaunchedEffect(Unit) {
                        onDispose()
                    }
                }
                @Composable
                fun Something(onDispose: () -> Unit) {
                    DisposableEffect(Unit) {
                        onDispose(onDispose)
                    }
                }
                @Composable
                fun Something(onDispose: () -> Unit) {
                    LifecycleStartEffect(Unit) {
                        onStopOrDispose(onDispose)
                    }
                }
                @Composable
                fun Something(onDispose: () -> Unit) {
                    LifecycleResumeEffect(Unit) {
                        onPauseOrDispose(onDispose)
                    }
                }

                // TODO ideally these would also be caught, but may require type resolution
                @Composable
                fun Something(onDispose: () -> Unit) {
                    DisposableEffect(Unit) {
                        onDispose { onDispose() }
                    }
                }
                @Composable
                fun Something(onDispose: (Int) -> Unit) {
                    DisposableEffect(Unit) {
                        onDispose(0)
                        onDispose {}
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 15),
                SourceLocation(8, 15),
                SourceLocation(14, 15),
                SourceLocation(20, 15),
            )
        for (error in errors) {
            assertThat(error).hasMessage(LambdaParameterInRestartableEffect.LambdaUsedInRestartableEffect)
        }
    }

    @Test
    fun `passes when a lambda named onDispose is present but unused in DisposableEffect`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onDispose: () -> Unit) {
                    val latestOnDispose by rememberUpdatedState(onDispose)
                    DisposableEffect(Unit) {
                        onDispose(latestOnDispose)
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when a lambda named onStopOrDispose is present but unused in LifecycleStartEffect`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onDispose: () -> Unit) {
                    val latestOnDispose by rememberUpdatedState(onDispose)
                    LifecycleStartEffect(Unit) {
                        onStopOrDispose(latestOnDispose)
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when a lambda named onPauseOrDispose is present but unused in LifecycleResumeEffect`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(onDispose: () -> Unit) {
                    val latestOnDispose by rememberUpdatedState(onDispose)
                    LifecycleResumeEffect(Unit) {
                        onPauseOrDispose(latestOnDispose)
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
