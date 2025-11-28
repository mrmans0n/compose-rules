// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.SourceLocation
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.nlopez.compose.rules.ModifierNotUsedAtRoot.Companion.ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierNotUsedAtRootCheckTest {

    private val testConfig = TestConfig(
        "contentEmitters" to listOf("Potato", "Banana"),
        "contentEmittersDenylist" to listOf("Apple"),
    )
    private val rule = ModifierNotUsedAtRootCheck(testConfig)

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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(4, 20),
                SourceLocation(10, 20),
                SourceLocation(17, 20),
                SourceLocation(24, 25),
                SourceLocation(28, 27),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace)
        }
    }

    @Test
    fun `passes out when modifier is used in too deep in the hierarchy but has a non-emitter parent`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    Dialog {
                        Text("Hi", modifier = modifier)
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    Apple {
                        Text("Hi", modifier = modifier)
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
                @Composable
                fun Something(
                  modifier: Modifier = Modifier,
                  content: @Composable BoxScope.() -> Unit
                ) {
                  MaterialTheme(
                    colorScheme = darkColorScheme()
                  ) {
                    Box(
                      modifier = modifier
                        .fillMaxSize()
                        .background(
                          color = MaterialTheme.colorScheme.background
                        )
                    ) {
                      Card(
                        modifier = Modifier.fillMaxSize()
                      ) {
                        Box(
                          modifier = Modifier.padding(16.dp)
                        ) {
                          content()
                        }
                      }
                    }
                  }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
