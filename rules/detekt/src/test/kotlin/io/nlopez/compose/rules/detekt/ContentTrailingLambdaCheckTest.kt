// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ContentTrailingLambda
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ContentTrailingLambdaCheckTest {

    private val testConfig = TestConfig(
        "treatAsComposableLambda" to listOf("Potato"),
        "treatAsLambda" to listOf("Plum"),
    )
    private val rule = ContentTrailingLambdaCheck(testConfig)

    @Test
    fun `errors when there is a content parameter not being last`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: @Composable () -> Unit, text: String) {}
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 7),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ContentTrailingLambda.ContentShouldBeTrailingLambda)
        }
    }

    @Test
    fun `errors when there is an inferred content parameter not being last`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: Potato, text: String) {}
                @Composable
                fun A(content: Apple, text: String) {}
                @Composable
                fun A(content: Banana, text: String) {}

                typealias Apple = @Composable () -> Unit

                fun interface Banana {
                    @Composable fun Content()
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 7),
                SourceLocation(4, 7),
                SourceLocation(6, 7),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ContentTrailingLambda.ContentShouldBeTrailingLambda)
        }
    }

    @Test
    fun `passes when content is the last parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(text: String, content: @Composable () -> Unit) {}
                @Composable
                fun A(content: @Composable Plum) {}
                @Composable
                fun A(text: String, content: Potato) {}
                @Composable
                fun A(text: String, content: Banana) {}
                @Composable
                fun A(text: String, content: Apple) {}

                typealias Apple = @Composable () -> Unit

                fun interface Banana {
                    @Composable fun Content()
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
