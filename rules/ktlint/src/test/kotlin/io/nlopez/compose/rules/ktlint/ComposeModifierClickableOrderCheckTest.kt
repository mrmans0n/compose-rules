// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ComposeModifierClickableOrder
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposeModifierClickableOrderCheckTest {

    private val modifierRuleAssertThat = assertThatRule { ComposeModifierClickableOrderCheck() }

    @Test
    fun `errors when there is a suspicious chain of modifiers`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(modifier: Modifier = Modifier) {
                    Something2(
                        modifier = Modifier.clickable { }.clip(shape = RoundedCornerShape(8.dp))
                    )
                    Something3(
                        modifier = modifier.clickable { }.clip(CircleShape())
                    )
                    Something4(
                        Modifier.clickable { }.clip(MyShape)
                    )
                    Something5(
                        modifier = Modifier.clip(CircleShape).clickable { }.background(MyShape)
                    )
                    Something6(
                        modifier.clickable { }.then(if (x) border(TurdShape) else Modifier)
                    )
                }
            """.trimIndent()
        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 4,
                col = 29,
                detail = ComposeModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 7,
                col = 29,
                detail = ComposeModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 10,
                col = 18,
                detail = ComposeModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 13,
                col = 47,
                detail = ComposeModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 16,
                col = 18,
                detail = ComposeModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
        )
    }

    @Test
    fun `passes with the correct order of modifiers`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1() {
                    Something2(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(shape = Circle()).clickable { }
                    )
                    Something2(
                        modifier = Modifier.clip(shape = Whatever).background().clickable { }
                    )
                }
            """.trimIndent()

        modifierRuleAssertThat(code).hasNoLintViolations()
    }
}
