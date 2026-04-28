// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.CompositionLocalNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class CompositionLocalNamingCheckTest {

    private val ruleAssertThat = assertThatRule { CompositionLocalNamingCheck() }

    @Test
    fun `error when a CompositionLocal has a wrong name`() {
        @Language("kotlin")
        val code =
            """
                val AppleLocal = staticCompositionLocalOf<String> { "Apple" }
                val Plum: ProvidableCompositionLocal<String> = staticCompositionLocalOf { "Plum" }
                private val Lemon = compositionLocalWithComputedDefaultOf { "Lemon" }
            """.trimIndent()
        ruleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 1,
                    col = 5,
                    detail = CompositionLocalNaming.CompositionLocalNeedsLocalPrefix,
                ),
                LintViolation(
                    line = 2,
                    col = 5,
                    detail = CompositionLocalNaming.CompositionLocalNeedsLocalPrefix,
                ),
                LintViolation(
                    line = 3,
                    col = 13,
                    detail = CompositionLocalNaming.CompositionLocalNeedsLocalPrefix,
                ),
            )
    }

    @Test
    fun `passes when a CompositionLocal is well named`() {
        @Language("kotlin")
        val code =
            """
                val LocalBanana = staticCompositionLocalOf<String> { "Banana" }
                val LocalPotato = compositionLocalOf { "Potato" }
                val LocalPeach = compositionLocalWithComputedDefaultOf { "Peach" }
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }
}
