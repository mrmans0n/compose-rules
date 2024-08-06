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
                @Composable
                fun Something(title: String?, subtitle: String?) {
                    with(title) { Text(this) }
                    with(subtitle) { Text(this) }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(7, 5),
                SourceLocation(12, 5),
                SourceLocation(17, 5),
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
    fun `errors when a Composable function emits multiple content with conditionals`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A() {
                    if (something) {
                        Text("1")
                        Text("2")
                    } else {
                        Text("1")
                        Text("2")
                    }
                }
                @Composable
                fun B() {
                    if (something) {
                        Text("1")
                        Text("2")
                    } else {
                        Text("1")
                    }
                }
                @Composable
                fun C() {
                    if (something) {
                        Text("1")
                    } else {
                        Text("1")
                        Text("2")
                    }
                }
                @Composable
                fun D() {
                    if (something) {
                        Text("1")
                    }
                    Text("2")
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(12, 5),
                SourceLocation(21, 5),
                SourceLocation(30, 5),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MultipleContentEmitters.MultipleContentEmittersDetected)
        }
    }

    @Test
    fun `errors when a Composable function emits multiple content with elvis operators`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A() {
                    text?.let {
                        Text("1")
                        Text("2")
                    } ?: run {
                        Text("1")
                        Text("2")
                    }
                }
                @Composable
                fun B() {
                    text?.let {
                        Text("1")
                        Text("2")
                    } ?: Text("1")
                }
                @Composable
                fun C() {
                    text?.let {
                        Text("1")
                    } ?: run {
                        Text("1")
                        Text("2")
                    }
                }
                @Composable
                fun D() {
                    text?.let { Text("1") }
                    Text("2")
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(12, 5),
                SourceLocation(19, 5),
                SourceLocation(28, 5),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MultipleContentEmitters.MultipleContentEmittersDetected)
        }
    }

    @Test
    fun `errors when a Composable function emits multiple content with a when`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A() {
                    when {
                        isPotato -> {
                            Text("1")
                            Text("2")
                        }
                        else -> {
                            Text("1")
                            Text("2")
                        }
                    }
                }
                @Composable
                fun B() {
                    when {
                        isPotato -> {
                            Text("1")
                        }
                        else -> {
                            Text("1")
                            Text("2")
                        }
                    }
                }
                @Composable
                fun C() {
                    when {
                        isPotato -> {
                            Text("1")
                            Text("2")
                        }
                        else -> {
                            Text("1")
                        }
                    }
                }
                @Composable
                fun D() {
                    Text("1")
                    when {
                        isPotato -> Text("2")
                        else -> {}
                    }
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(15, 5),
                SourceLocation(27, 5),
                SourceLocation(39, 5),
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

    @Test
    fun `passes for early returns`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    if (x) {
                        Text("1")
                        return
                    }
                    Text("2")
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `multiple emitters are caught despite early returns`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    Text("1")
                    if (x) {
                        Text("2")
                        return
                    }
                }
                @Composable
                fun Something() {
                    if (x) {
                        Text("1")
                        Text("2")
                        return
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(10, 5),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MultipleContentEmitters.MultipleContentEmittersDetected)
        }
    }
}
