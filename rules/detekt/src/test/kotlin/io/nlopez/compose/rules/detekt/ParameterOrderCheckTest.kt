// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class ParameterOrderCheckTest {

    private val testConfig = TestConfig(
        "treatAsLambda" to listOf("LambdaType"),
    )
    private val rule = ParameterOrderCheck(testConfig)

    @Test
    fun `no errors when ordering is correct`() {
        @Language("kotlin")
        val code = """
            typealias TypealiasLambda = () -> Unit
            fun interface InterfaceLambda {
                fun whatever()
            }

            fun MyComposable(text1: String, modifier: Modifier = Modifier, other: String = "1", other2: String = "2") { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, other2: String = "2", other : String = "1") { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, trailing: () -> Unit) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: () -> Unit) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: LambdaType) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: LambdaType?) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: (() -> Unit)?) { }

            @Composable
            fun MyComposable(modifier: Modifier, text1: String, m2: Modifier = Modifier, trailing: (() -> Unit)?) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: TypealiasLambda) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: TypealiasLambda?) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: InterfaceLambda) { }

            @Composable
            fun MyComposable(text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: InterfaceLambda?) { }


        """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors found when ordering is wrong`() {
        @Language("kotlin")
        val code = """
            @Composable
            fun MyComposable(modifier: Modifier = Modifier, other: String, other2: String) { }

            @Composable
            fun MyComposable(text: String = "deffo", modifier: Modifier = Modifier) { }

            @Composable
            fun MyComposable(modifier: Modifier = Modifier, text: String = "123", modifier2: Modifier = Modifier) { }

            @Composable
            fun MyComposable(text: String = "123", modifier: Modifier = Modifier, lambda: () -> Unit) { }

            @Composable
            fun MyComposable(text1: String, m2: Modifier = Modifier, modifier: Modifier = Modifier, trailing: () -> Unit) { }

            @Composable
            fun MyComposable(text1: String, m2: Modifier = Modifier, modifier: Modifier = Modifier, trailing: NonFunctionalType) { }

            typealias NonFunctionalType = String
        """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(5, 5),
                SourceLocation(8, 5),
                SourceLocation(11, 5),
                SourceLocation(14, 5),
                SourceLocation(17, 5),
            )
    }
}
