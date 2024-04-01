// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ModifierComposed
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierComposedCheckTest {

    private val modifierRuleAssertThat = assertThatRule { ModifierComposedCheck() }

    @Test
    fun `errors when a composable Modifier extension is detected`() {
        @Language("kotlin")
        val code =
            """
                fun Modifier.something1(): Modifier = composed {}
                fun Modifier.something2() = composed {}
                fun Modifier.something3() {
                    return composed {}
                }
            """.trimIndent()

        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 1,
                col = 14,
                detail = ModifierComposed.ComposedModifier,
            ),
            LintViolation(
                line = 2,
                col = 14,
                detail = ModifierComposed.ComposedModifier,
            ),
            LintViolation(
                line = 3,
                col = 14,
                detail = ModifierComposed.ComposedModifier,
            ),
        )
    }
}
