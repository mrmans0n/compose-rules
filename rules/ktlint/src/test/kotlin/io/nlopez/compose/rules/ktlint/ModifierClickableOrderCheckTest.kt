// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ModifierClickableOrder
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierClickableOrderCheckTest {

    private val modifierRuleAssertThat = assertThatRule { ModifierClickableOrderCheck() }

    @Test
    fun `errors when there is a suspicious chain of modifiers`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(modifier: Modifier = Modifier, bananaModifier: Modifier = Modifier) {
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
                    Something7(
                        modifier = bananaModifier.clickable { }.clip(shape = RoundedCornerShape(8.dp))
                    )
                    Something8(
                        modifier = bananaModifier.clickable { }.clip(Potato)
                    )
                    Something9(
                        modifier = bananaModifier.clickable { }.background(MaterialTheme.shapes.large)
                    )
                }
            """.trimIndent()
        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 4,
                col = 29,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 7,
                col = 29,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 10,
                col = 18,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 13,
                col = 47,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 16,
                col = 18,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 19,
                col = 35,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 22,
                col = 35,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 25,
                col = 35,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
        )
    }

    @Test
    fun `errors when a clickable is before a shadow with shape`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1(modifier: Modifier = Modifier) {
                    Something2(
                        modifier = Modifier.clickable { }.shadow(8.dp, RoundedCornerShape(8.dp))
                    )
                    Something3(
                        modifier = modifier.clickable { }.shadow(elevation = 4.dp, shape = CircleShape)
                    )
                    Something4(
                        modifier.clickable { }.then(if (x) shadow(8.dp, MyShape) else Modifier)
                    )
                    Something5(
                        modifier.clickable { }.then(if (x) Modifier.shadow(8.dp, MyShape) else Modifier)
                    )
                    Something6(
                        modifier.clickable { }.then(if (x) Modifier.shadow(8.dp, MyShape).padding(4.dp) else Modifier)
                    )
                }
            """.trimIndent()
        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 4,
                col = 29,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 7,
                col = 29,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 10,
                col = 18,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 13,
                col = 18,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
            LintViolation(
                line = 16,
                col = 18,
                detail = ModifierClickableOrder.ModifierChainWithSuspiciousOrder,
            ),
        )
    }

    @Test
    fun `passes when shadow comes before clickable or shadow does not clip`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something1() {
                    Something2(
                        modifier = Modifier.shadow(8.dp, RoundedCornerShape(8.dp)).clickable { }
                    )
                    Something3(
                        modifier = Modifier.clickable { }.shadow(8.dp, MyShape, clip = false)
                    )
                    Something4(
                        modifier = Modifier.clickable { }.shadow(8.dp, MyShape, false)
                    )
                    Something5(
                        modifier = Modifier.clickable { }.shadow(0.dp, MyShape)
                    )
                    Something6(
                        modifier = Modifier.clickable { }.shadow(elevation = 0.dp, shape = MyShape)
                    )
                    Something7(
                        modifier = Modifier.clickable { }
                            .then(if (x) run { something(Modifier.clip(CircleShape)); Modifier } else Modifier)
                    )
                }
            """.trimIndent()

        modifierRuleAssertThat(code).hasNoLintViolations()
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
