// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ParameterNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ParameterNamingCheckTest {

    private val testConfig = TestConfig(
        "treatAsComposableLambda" to listOf("Potato"),
    )
    private val rule = ParameterNamingCheck(testConfig)

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

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 7),
                SourceLocation(4, 7),
                SourceLocation(6, 7),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ParameterNaming.LambdaParametersInPresentTense)
        }
    }

    @Test
    fun `ignores lambdas that don't start with on`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(blehWrote: () -> Unit, mehChanged: () -> Unit, potatoed: Potato) {}
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when param lambdas are in present tense`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(onClick: () -> Unit, onValueChange: (Int) -> Unit, onWrite: () -> Unit, onPotato: Potato) {}
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
