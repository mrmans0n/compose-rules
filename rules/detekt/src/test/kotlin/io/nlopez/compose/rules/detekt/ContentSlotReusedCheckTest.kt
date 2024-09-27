// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ContentSlotReused
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ContentSlotReusedCheckTest {

    private val testConfig = TestConfig(
        "treatAsComposableLambda" to listOf("Potato"),
        "treatAsLambda" to listOf("Plum"),
    )
    private val rule = ContentSlotReusedCheck(testConfig)

    @Test
    fun `errors when there is a slot being reused in different branches`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(text: String, content: @Composable () -> Unit) {
                    if (x) content() else content()
                }
                @Composable
                fun B(text: String, content: @Composable () -> Unit) {
                    when {
                        x -> content()
                        else -> content()
                    }
                }
                @Composable
                fun C(text: String, content: @Composable () -> Unit) {
                    potato?.let { content() } ?: content()
                }
                @Composable
                fun D(text: String, content: Potato) {
                    potato?.let { content() } ?: content()
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 21),
                SourceLocation(6, 21),
                SourceLocation(13, 21),
                SourceLocation(17, 21),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ContentSlotReused.ContentSlotReusedInDifferentBranches)
        }
    }

    @Test
    fun `passes when used in a movableContentOf`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: @Composable () -> Unit, text: String) {
                    val content = remember { movableContentOf { content() } }
                    if (x) content() else content()
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when content is not composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: () -> Unit, text: String) {
                    if (x) content() else content()
                }
                fun B(content: Plum, text: String) {
                    if (x) content() else content()
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
