// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ComposeModifierClickableOrder
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposeModifierClickableOrderCheckTest {

    private val rule = ComposeModifierClickableOrderCheck(Config.empty)

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

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(4, 29),
                SourceLocation(7, 29),
                SourceLocation(10, 18),
                SourceLocation(13, 47),
                SourceLocation(16, 18),
            )

        assertThat(errors[0]).hasMessage(ComposeModifierClickableOrder.ModifierChainWithSuspiciousOrder)
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

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
