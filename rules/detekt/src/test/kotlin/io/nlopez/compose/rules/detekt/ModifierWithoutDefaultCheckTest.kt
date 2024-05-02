// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ModifierWithoutDefault
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierWithoutDefaultCheckTest {

    private val rule = ModifierWithoutDefaultCheck(Config.empty)

    @Test
    fun `errors when a Composable has modifiers but without default values`() {
        @Language("kotlin")
        val composableCode = """
                @Composable
                fun Something(modifier: Modifier) { }
                @Composable
                fun Something(modifier: Modifier = Modifier, modifier2: Modifier) { }
        """.trimIndent()

        val errors = rule.lint(composableCode)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(2, 15),
            SourceLocation(4, 46),
        )
        assertThat(errors[0]).hasMessage(ModifierWithoutDefault.MissingModifierDefaultParam)
        assertThat(errors[1]).hasMessage(ModifierWithoutDefault.MissingModifierDefaultParam)
    }

    @Test
    fun `passes when a Composable inside of an interface has modifiers but without default values`() {
        @Language("kotlin")
        val composableCode = """
                interface Bleh {
                    @Composable
                    fun Something(modifier: Modifier)
                }
                class BlehImpl : Bleh {
                    @Composable
                    override fun Something(modifier: Modifier) {}
                }
                @Composable
                actual fun Something(modifier: Modifier) {}
        """.trimIndent()

        val errors = rule.lint(composableCode)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when a Composable is an abstract function but without default values`() {
        @Language("kotlin")
        val composableCode = """
                abstract class Bleh {
                    @Composable
                    abstract fun Something(modifier: Modifier)

                    @Composable
                    open fun Something(modifier: Modifier) {}
                }
        """.trimIndent()

        val errors = rule.lint(composableCode)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when a Composable has modifiers with defaults`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(modifier: Modifier = Modifier) {
                    Row(modifier = modifier) {
                    }
                }
                @Composable
                fun Something(modifier: Modifier = Modifier.fillMaxSize()) {
                    Row(modifier = modifier) {
                    }
                }
                @Composable
                fun Something(modifier: Modifier = SomeOtherValueFromSomeConstant) {
                    Row(modifier = modifier) {
                    }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
