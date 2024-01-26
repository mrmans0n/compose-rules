// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.MultipleContentEmitters
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MultipleContentEmittersCheckTest {

    private val testConfig = TestConfig(
        "contentEmitters" to listOf("Potato", "Banana", "Apple"),
        "contentEmittersDenylist" to listOf("Apple"),
    )
    private val rule = MultipleContentEmittersCheck(testConfig)

    @Test
    fun `passes when only one item emits up at the top level`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    val something = rememberWhatever()
                    Column {
                        Text("Hi")
                        Text("Hola")
                    }
                    LaunchedEffect(Unit) {
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when the composable is an extension function`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun ColumnScope.Something() {
                    Text("Hi")
                    Text("Hola")
                }
                @Composable
                fun RowScope.Something() {
                    Spacer()
                    Text("Hola")
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when the composable is a context receiver`() {
        @Language("kotlin")
        val code =
            """
                context(ColumnScope)
                @Composable
                fun Something() {
                    Text("Hi")
                    Text("Hola")
                }
                context(ColumnScope)
                @Composable
                fun Something() {
                    Spacer()
                    Text("Hola")
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors when a Composable function has more than one UI emitter at the top level`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    Text("Hi")
                    Text("Hola")
                }
                @Composable
                fun Something() {
                    Spacer()
                    Text("Hola")
                }
                @Composable
                fun Something(title: String?, subtitle: String?) {
                    title?.let { Text(title) }
                    subtitle?.let { Text(subtitle) }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(7, 5),
                SourceLocation(12, 5),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MultipleContentEmitters.MultipleContentEmittersDetected)
        }
    }

    @Test
    fun `errors when a Composable function has more than one indirect UI emitter at the top level`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1() {
                    Something2()
                }
                @Composable
                fun Something2() {
                    Text("Hola")
                    Something3()
                }
                @Composable
                fun Something3() {
                    Potato()
                }
                @Composable
                fun Something4() {
                    Banana()
                }
                @Composable
                fun Something5() {
                    Something3()
                    Something4()
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(6, 5),
                SourceLocation(19, 5),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MultipleContentEmitters.MultipleContentEmittersDetected)
        }
    }

    @Test
    fun `make sure to not report twice the same composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    Text("Hi")
                    Text("Hola")
                    Something2()
                }
                @Composable
                fun Something2() {
                    Text("Alo")
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocation(2, 5)
        assertThat(errors.first()).hasMessage(MultipleContentEmitters.MultipleContentEmittersDetected)
    }

    @Test
    fun `for loops are captured`() {
        @Language("kotlin")
        val code = """
            @Composable
            fun MultipleContent(texts: List<String>, modifier: Modifier = Modifier) {
                for (text in texts) {
                    Text(text)
                }
            }
            @Composable
            fun MultipleContent(otherTexts: List<String>, modifier: Modifier = Modifier) {
                Text("text 1")
                for (otherText in otherTexts) {
                    Text(otherText)
                }
            }
        """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(8, 5),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MultipleContentEmitters.MultipleContentEmittersDetected)
        }
    }

    @Test
    fun `passes when the composable is in the denylist`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    Text("Hi")
                    Apple()
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
