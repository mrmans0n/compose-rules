// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ModifierNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierNamingCheckTest {

    private val modifierRuleAssertThat = assertThatRule { ModifierNamingCheck() }

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

        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 16,
                detail = ModifierNaming.ModifiersAreSupposedToBeCalledModifierWhenAlone,
            ),
            LintViolation(
                line = 4,
                col = 16,
                detail = ModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple,
            ),
            LintViolation(
                line = 4,
                col = 29,
                detail = ModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple,
            ),
        )
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

        modifierRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when a Composable has a single modifier not named modifier but ends with modifier`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(myModifier: Modifier) {}
            """.trimIndent()

        modifierRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(
            line = 2,
            col = 16,
            detail = ModifierNaming.ModifiersAreSupposedToBeCalledModifierWhenAlone,
        )
    }
}
