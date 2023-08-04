// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ComposeModifierNotUsedAtRoot.Companion.ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposeModifierNotUsedAtRootCheckTest {

    private val modifierRuleAssertThat = assertThatRule { ComposeModifierNotUsedAtRootCheck() }

    @Test
    fun `error out when modifier is used in too deep in the hierarchy`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    Row {
                        Text("Hi", modifier = modifier)
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    Potato(Modifier.fillMaxWidth()) {
                        Text("Hi", modifier = modifier)
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    val poop = if (x) modifier else modifier.fillMaxWidth()
                    Column {
                        Text("Hi", modifier = poop)
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    if (paella.isWellDone()) {
                        Column {
                            Text("Yay", modifier)
                        }
                    } else {
                        Row {
                            Text("Oh no", modifier)
                        }
                    }
                }

            """.trimIndent()
        modifierRuleAssertThat(code)
            .withEditorConfigOverride(
                contentEmittersProperty to "Potato,Banana",
            )
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 4,
                    col = 20,
                    detail = ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace,
                ),
                LintViolation(
                    line = 10,
                    col = 20,
                    detail = ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace,
                ),
                LintViolation(
                    line = 17,
                    col = 20,
                    detail = ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace,
                ),
                LintViolation(
                    line = 24,
                    col = 25,
                    detail = ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace,
                ),
                LintViolation(
                    line = 28,
                    col = 27,
                    detail = ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace,
                ),
            )
    }

    @Test
    fun `passes when modifier is used in the top-most place that emits content`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    Row(modifier = modifier) {
                        Text("Hi")
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    Potato(modifier.fillMaxWidth()) {
                        Text("Hi")
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    val poop = if (x) modifier else modifier.fillMaxWidth()
                    Column(modifier = poop) {
                        Text("Hi")
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    if (paella.isWellDone()) {
                        Column(modifier) {
                            Text("Yay")
                        }
                    } else {
                        Row(modifier) {
                            Text("Oh no")
                        }
                    }
                }

            """.trimIndent()
        modifierRuleAssertThat(code)
            .withEditorConfigOverride(
                contentEmittersProperty to "Potato,Banana",
            )
            .hasNoLintViolations()
    }
}
