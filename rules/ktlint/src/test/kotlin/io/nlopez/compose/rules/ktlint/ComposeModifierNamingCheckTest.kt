// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ComposeModifierNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposeModifierNamingCheckTest {

    private val modifierRuleAssertThat = assertThatRule { ComposeModifierNamingCheck() }

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
                detail = ComposeModifierNaming.ModifiersAreSupposedToBeCalledModifierWhenAlone,
            ),
            LintViolation(
                line = 4,
                col = 16,
                detail = ComposeModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple,
            ),
            LintViolation(
                line = 4,
                col = 29,
                detail = ComposeModifierNaming.ModifiersAreSupposedToEndInModifierWhenMultiple,
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
}
