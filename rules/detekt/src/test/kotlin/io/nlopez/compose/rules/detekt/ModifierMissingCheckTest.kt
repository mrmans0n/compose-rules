// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ModifierMissing
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierMissingCheckTest {

    private val testConfig = TestConfig(
        "customModifiers" to listOf("BananaModifier"),
        "contentEmittersDenylist" to listOf("PotatoDialog"),
    )
    private val rule = ModifierMissingCheck(testConfig)

    @Test
    fun `errors when a Composable has a layout inside and it doesn't have a modifier`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1() {
                    Row {
                    }
                }
                @Composable
                fun Something2() {
                    Column(modifier = Modifier.fillMaxSize()) {
                    }
                }
                @Composable
                fun Something3(): Unit {
                    SomethingElse {
                        Box(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
                @Composable
                fun Something4(): Unit {
                    SomethingElse {
                        Box(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
                @Composable
                fun Something5(modifier: Modifier = Modifier) {
                    Row {
                        Text("Hi!")
                    }
                }
                @Composable
                fun Something6() {
                    Column(modifier = BananaModifier.fillMaxSize()) {
                    }
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).hasTextLocations("Something1", "Something2", "Something3", "Something4", "Something6")
        for (error in errors) {
            assertThat(error).hasMessage(ModifierMissing.MissingModifierContentComposable)
        }
    }

    @Test
    fun `errors when a Composable without modifiers has a Composable inside with a modifier`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1() {
                    Whatever(modifier = Modifier.fillMaxSize()) {
                    }
                }
                @Composable
                fun Something2(): Unit {
                    SomethingElse {
                        Whatever(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).hasTextLocations("Something1", "Something2")
        assertThat(errors[0]).hasMessage(ModifierMissing.MissingModifierContentComposable)
        assertThat(errors[1]).hasMessage(ModifierMissing.MissingModifierContentComposable)
    }

    @Test
    fun `non-public visibility Composables are ignored (by default)`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                private fun Something() {
                    Row {
                    }
                }
                @Composable
                protected fun Something() {
                    Column(modifier = Modifier.fillMaxSize()) {
                    }
                }
                @Composable
                internal fun Something() {
                    SomethingElse {
                        Box(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
                @Composable
                private fun Something() {
                    Whatever(modifier = Modifier.fillMaxSize()) {
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `public and internal visibility Composables are checked for 'public_and_internal' configuration`() {
        val newRule = ModifierMissingCheck(
            TestConfig("checkModifiersForVisibility" to "public_and_internal"),
        )

        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    Row {
                    }
                }
                @Composable
                protected fun Something() {
                    Column(modifier = Modifier.fillMaxSize()) {
                    }
                }
                @Composable
                internal fun Something() {
                    SomethingElse {
                        Box(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
                @Composable
                private fun Something() {
                    Whatever(modifier = Modifier.fillMaxSize()) {
                    }
                }
            """.trimIndent()
        val errors = newRule.lint(code)
        assertThat(errors).hasSize(2)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(12, 14),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ModifierMissing.MissingModifierContentComposable)
        }
    }

    @Test
    fun `all Composables are checked for 'all' configuration`() {
        val newRule = ModifierMissingCheck(
            TestConfig("checkModifiersForVisibility" to "all"),
        )

        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    Row {
                    }
                }
                @Composable
                protected fun Something() {
                    Column(modifier = Modifier.fillMaxSize()) {
                    }
                }
                @Composable
                internal fun Something() {
                    SomethingElse {
                        Box(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
                @Composable
                private fun Something() {
                    Whatever(modifier = Modifier.fillMaxSize()) {
                    }
                }
            """.trimIndent()
        val errors = newRule.lint(code)
        assertThat(errors).hasSize(4)
            .hasStartSourceLocations(
                SourceLocation(2, 5),
                SourceLocation(7, 15),
                SourceLocation(12, 14),
                SourceLocation(19, 13),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ModifierMissing.MissingModifierContentComposable)
        }
    }

    @Test
    fun `interface Composables are ignored`() {
        @Language("kotlin")
        val code =
            """
                interface MyInterface {
                    @Composable
                    fun Something() {
                        Row {
                        }
                    }

                    @Composable
                    fun Something() {
                        Column(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `overridden Composables are ignored`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                override fun Content() {
                    Row {
                    }
                }
                @Composable
                override fun TwitterContent() {
                    Row {
                    }
                }
                @Composable
                override fun ModalContent() {
                    Row {
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `Composables that return a type that is not Unit shouldn't be processed`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(): Int {
                    Row {
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `Composables with @Preview are ignored`() {
        @Language("kotlin")
        val code =
            """
                @Preview
                @Composable
                fun Something() {
                    Row {
                    }
                }
                @PreviewScreenSizes
                @Composable
                fun Something() {
                    Row {
                    }
                }
                @Preview
                @Composable
                fun Something() {
                    Column(modifier = Modifier.fillMaxSize()) {
                    }
                }
                @Preview
                @Composable
                fun Something(): Unit {
                    SomethingElse {
                        Box(modifier = Modifier.fillMaxSize()) {
                        }
                    }
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `non content emitting root composables are ignored`() {
        @Language("kotlin")
        val code =
            """
                fun MyDialog() {
                    AlertDialog(
                        onDismissRequest = { /*TODO*/ },
                        buttons = { Text(text = "Button") },
                        text = { Text(text = "Body") },
                    )
                    PotatoDialog(
                        onDismissRequest = { /*TODO*/ },
                        buttons = { Text(text = "Button") },
                        text = { Text(text = "Body") },
                    )
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `non content emitter with content emitter not ignored`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyDialog() {
                    Text(text = "Unicorn")

                    AlertDialog(
                        onDismissRequest = { /*TODO*/ },
                        buttons = { Text(text = "Button") },
                        text = { Text(text = "Body") },
                    )
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).hasTextLocations("MyDialog")
        assertThat(errors[0]).hasMessage(ModifierMissing.MissingModifierContentComposable)
    }

    @Test
    fun `Modifier factory functions are ignored`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Modifier.Something() {
                    Row {
                    }
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
