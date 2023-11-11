// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ModifierComposable
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierComposableCheckTest {

    private val rule = ModifierComposableCheck(Config.empty)

    @Test
    fun `errors when a composable Modifier extension is detected`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Modifier.something1(): Modifier { }
                @Composable
                fun Modifier.something2() = somethingElse()
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasTextLocations("something1", "something2")
        assertThat(errors[0]).hasMessage(ModifierComposable.ComposableModifier)
        assertThat(errors[1]).hasMessage(ModifierComposable.ComposableModifier)
    }

    @Test
    fun `do not error on a regular composable`() {
        @Language("kotlin")
        val code = """
            @Composable
            fun TextHolder(text: String) {}
        """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
