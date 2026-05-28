// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.nlopez.compose.rules.ComposableNestingDepth
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposableNestingDepthCheckTest {

    private val rule = ComposableNestingDepthCheck(Config.empty)

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
        assertThat(rule.lint(code)).isEmpty()
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
        assertThat(rule.lint(code)).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(SourceLocation(2, 5))
        for (error in errors) {
            assertThat(error).hasMessage(ComposableNestingDepth.ComposableTooDeeplyNested)
        }
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
        assertThat(rule.lint(code)).hasStartSourceLocations(
            SourceLocation(2, 5),
            SourceLocation(10, 5),
        )
    }

    @Test
    fun `respects a custom threshold via config`() {
        val customConfig = TestConfig("composableNestingDepthThreshold" to 1)
        val customRule = ComposableNestingDepthCheck(customConfig)

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
        assertThat(customRule.lint(code)).hasStartSourceLocations(SourceLocation(2, 5))
    }

    @Test
    fun `passes when threshold is raised to allow deep nesting`() {
        val customConfig = TestConfig("composableNestingDepthThreshold" to 10)
        val customRule = ComposableNestingDepthCheck(customConfig)

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
        assertThat(customRule.lint(code)).isEmpty()
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
        assertThat(rule.lint(code)).isEmpty()
    }

    @Test
    fun `does not flag functions without a block body`() {
        @Language("kotlin")
        val code =
            """
                val Content: @Composable () -> Unit = { Text("hi") }
            """.trimIndent()
        assertThat(rule.lint(code)).isEmpty()
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
        assertThat(rule.lint(code)).hasStartSourceLocations(SourceLocation(4, 9))
    }

    @Test
    fun `analyzes expression-bodied composables for nesting depth`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun TooDeep() = Box { Box { Box { Box { Text("x") } } } }
                @Composable
                fun WithinLimit() = Box { Box { Box { Text("x") } } }
            """.trimIndent()
        // TooDeep nests 4 emitters around Text (> threshold) and is flagged.
        // WithinLimit nests 3 (== threshold) and is intentionally absent from the expected locations.
        assertThat(rule.lint(code)).hasStartSourceLocations(SourceLocation(2, 5))
    }
}
