// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ModifierShouldBeForwardedToEveryBranchCheckTest {
    private val modifierRuleAssertThat = assertThatRule { ModifierShouldBeForwardedToEveryBranchCheck() }

    @ParameterizedTest
    @ValueSource(strings = ["modifier", "((modifier))", "(modifier.padding())", "Modifier.then((modifier))"])
    fun `reports a modifier omitted from a root when branch`(forwarded: String) {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Content(state: Int, modifier: Modifier = Modifier) {
                    when (state) {
                        0 -> LoadedContent($forwarded)
                        else -> LoadingContent()
                    }
                }
                @Composable
                fun LoadedContent(modifier: Modifier = Modifier) {}
                @Composable
                fun LoadingContent(modifier: Modifier = Modifier) {}
            """.trimIndent()
        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                5,
                17,
                "One branch forwards the modifier parameter, but this branch does not. Pass it to every branch whose composable accepts a modifier.",
            ),
        )
    }

    @Test
    fun `reports a fresh positional modifier in a nested root branch`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Content(state: Int, modifier: Modifier = Modifier) {
                    if (state == 0) {
                        LoadedContent("ready", modifier.padding(8.dp))
                    } else if (state == 1) {
                        LoadingContent("loading", Modifier)
                    } else {
                        LoadedContent("ready", modifier = modifier)
                    }
                }
                @Composable
                fun LoadedContent(label: String, modifier: Modifier = Modifier) {}
                @Composable
                fun LoadingContent(label: String, rootModifier: Modifier = Modifier) {}
            """.trimIndent()
        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                6,
                9,
                "One branch forwards the modifier parameter, but this branch does not. Pass it to every branch whose composable accepts a modifier.",
            ),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "if (state == 0) LoadingContent(modifier) else LoadedContent(modifier)",
            "if (state == 0) LoadingContent() else LoadedContent()",
            "if (state == 0) EmptyContent() else LoadedContent(modifier)",
            "if (state == 0) ExternalContent() else LoadedContent(modifier)",
            "if (state == 0) Other.LoadingContent() else LoadedContent(modifier)",
            "if (state == 0) OverloadedContent() else LoadedContent(modifier)",
            "if (state == 0) LoadingContent(unknownModifier) else LoadedContent(modifier)",
            "when (val modifier = otherModifier) { Modifier -> LoadingContent(); else -> LoadedContent(modifier) }",
            "if (state == 0) MixedContent(label = \"loading\", modifier) else LoadedContent(modifier)",
            "if (state == 0) LoadingContent(if (enabled) modifier else Modifier) else LoadedContent(modifier)",
            "Box(modifier) { if (state == 0) LoadingContent() else LoadedContent() }",
            "Header()\nif (state == 0) LoadingContent() else LoadedContent(modifier)",
        ],
    )
    fun `skips branches without a definite missing modifier`(body: String) {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Content(state: Int, modifier: Modifier = Modifier) {
                    $body
                }
                @Composable
                fun LoadedContent(modifier: Modifier = Modifier) {}
                @Composable
                fun LoadingContent(modifier: Modifier = Modifier) {}
                @Composable
                fun EmptyContent() {}
                @Composable
                fun MixedContent(label: String, modifier: Modifier = Modifier) {}
                @Composable
                fun OverloadedContent(modifier: Modifier = Modifier) {}
                @Composable
                fun OverloadedContent(label: String = "", modifier: Modifier = Modifier) {}
            """.trimIndent()
        modifierRuleAssertThat(code).hasNoLintViolations()
    }
}
