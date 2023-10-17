// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ComposeModifierNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposeModifierNamingCheckTest {

    private val rule = ComposeModifierNamingCheck(Config.empty)

    @Test
    fun `errors when a Composable has a modifier not named modifier`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(m: Modifier) {}
                @Composable
                fun Something2(m: Modifier, m2: Modifier) {}
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 16),
                SourceLocation(4, 16),
                SourceLocation(4, 29),
            )

        assertThat(errors[0]).hasMessage(ComposeModifierNaming.ModifiersAreSupposedToBeCalledModifierWhenAlone)
        assertThat(errors[1]).hasMessage(ComposeModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple)
        assertThat(errors[2]).hasMessage(ComposeModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple)
    }

    @Test
    fun `passes when the modifiers are named correctly`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(modifier: Modifier) {}
                @Composable
                fun Something2(modifier: Modifier, otherModifier: Modifier) {}
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors when a Composable has a single modifier not named modifier but ends with modifier`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(myModifier: Modifier) {}
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocation(2, 16)

        assertThat(errors[0]).hasMessage(ComposeModifierNaming.ModifiersAreSupposedToBeCalledModifierWhenAlone)
    }

    @Test
    fun `errors when a Composable has a non first modifier named exactly modifier`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(myModifier: Modifier, modifier: Modifier) {}
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 16),
                SourceLocation(2, 38),
            )

        assertThat(errors[0]).hasMessage(ComposeModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple)
        assertThat(errors[1]).hasMessage(ComposeModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple)
    }
}
