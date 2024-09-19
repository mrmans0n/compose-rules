// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.LambdaParameterEventTrailing
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class LambdaParameterEventTrailingCheckTest {

    private val rule = LambdaParameterEventTrailingCheck(Config.empty)

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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 46),
            )
        for (error in errors) {
            assertThat(error).hasMessage(LambdaParameterEventTrailing.EventLambdaIsTrailingLambda)
        }
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when the function doesnt emit content`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun something(onClick: () -> Unit) {}
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
