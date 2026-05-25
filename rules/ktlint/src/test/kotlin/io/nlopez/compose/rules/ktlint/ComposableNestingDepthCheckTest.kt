// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ComposableNestingDepth
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposableNestingDepthCheckTest {

    private val nestingRuleAssertThat = assertThatRule { ComposableNestingDepthCheck() }

    @Test
    fun `passes when there is no nesting`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Plain() {
                    Text("hello")
                }
            """.trimIndent()
        nestingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes at the default threshold`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun WithinLimit() {
                    Box {
                        Box {
                            Box {
                                Bar()
                            }
                        }
                    }
                }
            """.trimIndent()
        nestingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when nesting exceeds the default threshold`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun TooDeep() {
                    Box {
                        Box {
                            Box {
                                Box {
                                    Box {
                                        Text("")
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
        nestingRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 5,
                detail = ComposableNestingDepth.ComposableTooDeeplyNested,
            ),
        )
    }

    @Test
    fun `errors per function in the file`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun TooDeepA() {
                    Box { Box { Box { Box { Text("a") } } } }
                }
                @Composable
                fun ShallowB() {
                    Box { Text("b") }
                }
                @Composable
                fun TooDeepC() {
                    Column { Row { Box { Box { Text("c") } } } }
                }
            """.trimIndent()
        nestingRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 5,
                detail = ComposableNestingDepth.ComposableTooDeeplyNested,
            ),
            LintViolation(
                line = 10,
                col = 5,
                detail = ComposableNestingDepth.ComposableTooDeeplyNested,
            ),
        )
    }

    @Test
    fun `respects a custom threshold via editorconfig`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun BarelyOk() {
                    Box {
                        Box {
                            Text("hello")
                        }
                    }
                }
            """.trimIndent()
        nestingRuleAssertThat(code)
            .withEditorConfigOverride(composableNestingDepthThreshold to 1)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 5,
                    detail = ComposableNestingDepth.ComposableTooDeeplyNested,
                ),
            )
    }

    @Test
    fun `passes when threshold is raised to allow deep nesting`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun PreviouslyTooDeep() {
                    Box {
                        Box {
                            Box {
                                Box {
                                    Text("hello")
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
        nestingRuleAssertThat(code)
            .withEditorConfigOverride(composableNestingDepthThreshold to 10)
            .hasNoLintViolations()
    }

    @Test
    fun `does not count non-Composable control flow toward nesting`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun WithControlFlow(items: List<String>, cond: Boolean) {
                    Box {
                        if (cond) {
                            for (item in items) {
                                Box {
                                    Text(item)
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
        nestingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `does not flag functions without a block body`() {
        @Language("kotlin")
        val code =
            """
                val Content: @Composable () -> Unit = { Text("hi") }
            """.trimIndent()
        nestingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `does not count nesting inside a locally-declared composable toward the outer function`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Outer() {
                    @Composable
                    fun Inner() {
                        Box {
                            Box {
                                Box {
                                    Box {
                                        Text("hi")
                                    }
                                }
                            }
                        }
                    }
                    Inner()
                }
            """.trimIndent()
        // Outer's body only calls Inner() (not an emitter), so it must not be flagged.
        // Inner itself has 4 nested Boxes around Text, so it should be flagged on its own.
        nestingRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 4,
                col = 9,
                detail = ComposableNestingDepth.ComposableTooDeeplyNested,
            ),
        )
    }
}
