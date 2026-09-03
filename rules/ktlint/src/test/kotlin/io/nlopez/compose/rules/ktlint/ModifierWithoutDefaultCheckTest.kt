// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ModifierWithoutDefault
import org.assertj.core.api.Assertions.assertThat
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
    fun `the autocorrect supports annotated parameters in multiline parameter lists`() {
        @Language("kotlin")
        val composableCode = """
                @Composable
                fun Something(
                    title: String,
                    @Suppress("unused") modifier: Modifier,
                ) { }
        """.trimIndent()

        modifierRuleAssertThat(composableCode)
            .hasLintViolation(
                line = 4,
                col = 25,
                detail = ModifierWithoutDefault.MissingModifierDefaultParam,
            )
            .isFormattedAs(
                """
                @Composable
                fun Something(
                    title: String,
                    @Suppress("unused") modifier: Modifier = Modifier,
                ) { }
                """.trimIndent(),
            )
    }

    @Test
    fun `the autocorrect keeps the Modifier import when the parameter type is its only usage`() {
        @Language("kotlin")
        val code = """
                package com.example.probe

                import androidx.compose.foundation.layout.Box
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier

                @Composable
                fun ProbeScreen(modifier: Modifier, title: String) {
                    Box(modifier = modifier) { }
                }
        """.trimIndent()

        val engine = KtLintRuleEngine(
            ruleProviders = setOf(RuleProvider { ModifierWithoutDefaultCheck() }) +
                StandardRuleSetProvider().getRuleProviders()
                    .filter { it.createNewRuleInstance().ruleId.value == "standard:no-unused-imports" },
        )

        val formatted = engine.format(Code.fromSnippet(code)) { AutocorrectDecision.ALLOW_AUTOCORRECT }

        assertThat(formatted)
            .contains("fun ProbeScreen(modifier: Modifier = Modifier, title: String)")
            .contains("import androidx.compose.ui.Modifier")
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

                    @Composable
                    open fun Something(modifier: Modifier) {}
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

    @Test
    fun `passes for Modifier factory functions`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Modifier.something(modifier: Modifier) {
                    Row(modifier = modifier) {
                    }
                }
            """.trimIndent()
        modifierRuleAssertThat(code).hasNoLintViolations()
    }
}
