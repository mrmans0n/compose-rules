// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ParameterOrder
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class ParameterOrderCheckTest {

    private val orderingRuleAssertThat = assertThatRule { ParameterOrderCheck() }

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
        orderingRuleAssertThat(code)
            .withEditorConfigOverride(treatAsLambda to "LambdaType")
            .hasNoLintViolations()
    }

    @Test
    fun `no errors when non-composable lambda with default value comes before required params - issue 416`() {
        @Language("kotlin")
        val code = """
            @Composable
            fun Foo(
              onSelect: (Foo) -> Unit,
              displayMapper: (Foo) -> String = { it.name },
              title: String,
            ) { }

            @Composable
            fun Bar(
              onClick: () -> Unit,
              formatter: (String) -> String = { it },
              text: String,
              count: Int,
            ) { }

            @Composable
            fun Baz(
              modifier: Modifier = Modifier,
              onValueChange: (Int) -> Unit = {},
              value: Int,
            ) { }
        """.trimIndent()
        orderingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `error when composable lambda with default value comes before required params`() {
        @Language("kotlin")
        val code = """
            @Composable
            fun Foo(
              onClick: () -> Unit,
              content: @Composable () -> Unit = {},
              title: String,
            ) { }
        """.trimIndent()
        orderingRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 5,
                detail = ParameterOrder.createErrorMessage(
                    currentOrder = "onClick: () -> Unit, content: @Composable () -> Unit = {}, title: String",
                    properOrder = "onClick: () -> Unit, title: String, content: @Composable () -> Unit = {}",
                ),
            ),
        )
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
        orderingRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 5,
                detail = ParameterOrder.createErrorMessage(
                    currentOrder = "modifier: Modifier = Modifier, other: String, other2: String",
                    properOrder = "other: String, other2: String, modifier: Modifier = Modifier",
                ),
            ),
            LintViolation(
                line = 5,
                col = 5,
                detail = ParameterOrder.createErrorMessage(
                    currentOrder = "text: String = \"deffo\", modifier: Modifier = Modifier",
                    properOrder = "modifier: Modifier = Modifier, text: String = \"deffo\"",
                ),
            ),
            LintViolation(
                line = 8,
                col = 5,
                detail = ParameterOrder.createErrorMessage(
                    currentOrder =
                    "modifier: Modifier = Modifier, text: String = \"123\", modifier2: Modifier = Modifier",
                    properOrder =
                    "modifier: Modifier = Modifier, modifier2: Modifier = Modifier, text: String = \"123\"",
                ),
            ),
            LintViolation(
                line = 11,
                col = 5,
                detail = ParameterOrder.createErrorMessage(
                    currentOrder = "text: String = \"123\", modifier: Modifier = Modifier, lambda: () -> Unit",
                    properOrder = "modifier: Modifier = Modifier, text: String = \"123\", lambda: () -> Unit",
                ),
            ),
            LintViolation(
                line = 14,
                col = 5,
                detail = ParameterOrder.createErrorMessage(
                    currentOrder =
                    "text1: String, m2: Modifier = Modifier, modifier: Modifier = Modifier, trailing: () -> Unit",
                    properOrder =
                    "text1: String, modifier: Modifier = Modifier, m2: Modifier = Modifier, trailing: () -> Unit",
                ),
            ),
            LintViolation(
                line = 17,
                col = 5,
                detail = ParameterOrder.createErrorMessage(
                    currentOrder =
                    "text1: String, m2: Modifier = Modifier, modifier: Modifier = Modifier, trailing: NonFunctionalType",
                    properOrder =
                    "text1: String, trailing: NonFunctionalType, modifier: Modifier = Modifier, m2: Modifier = Modifier",
                ),
            ),
        )
    }
}
