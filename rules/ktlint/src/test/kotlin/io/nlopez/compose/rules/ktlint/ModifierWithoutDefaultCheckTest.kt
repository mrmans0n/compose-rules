// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ModifierWithoutDefault
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierWithoutDefaultCheckTest {

    private val modifierRuleAssertThat = assertThatRule { ModifierWithoutDefaultCheck() }

    @Test
    fun `errors when a Composable has modifiers but without default values, and is able to auto fix it`() {
        @Language("kotlin")
        val composableCode = """
                @Composable
                fun Something(modifier: Modifier) { }
                @Composable
                fun Something(modifier: Modifier = Modifier, modifier2: Modifier) { }
        """.trimIndent()

        modifierRuleAssertThat(composableCode)
            .hasLintViolations(
                LintViolation(
                    line = 2,
                    col = 15,
                    detail = ModifierWithoutDefault.MissingModifierDefaultParam,
                ),
                LintViolation(
                    line = 4,
                    col = 46,
                    detail = ModifierWithoutDefault.MissingModifierDefaultParam,
                ),
            )
            .isFormattedAs(
                """
                @Composable
                fun Something(modifier: Modifier = Modifier) { }
                @Composable
                fun Something(modifier: Modifier = Modifier, modifier2: Modifier = Modifier) { }
                """.trimIndent(),
            )
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

        modifierRuleAssertThat(composableCode).hasNoLintViolations()
    }

    @Test
    fun `passes when a Composable is an abstract function but without default values`() {
        @Language("kotlin")
        val composableCode = """
                abstract class Bleh {
                    @Composable
                    abstract fun Something(modifier: Modifier)
                }
        """.trimIndent()

        modifierRuleAssertThat(composableCode).hasNoLintViolations()
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
        modifierRuleAssertThat(code).hasNoLintViolations()
    }
}
