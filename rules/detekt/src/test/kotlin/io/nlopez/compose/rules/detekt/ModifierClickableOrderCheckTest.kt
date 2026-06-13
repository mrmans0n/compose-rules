// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import dev.detekt.test.lint
import io.nlopez.compose.rules.ModifierClickableOrder
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierClickableOrderCheckTest {

    private val rule = ModifierClickableOrderCheck(Config.empty)

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

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(4, 29),
                SourceLocation(7, 29),
                SourceLocation(10, 18),
                SourceLocation(13, 47),
                SourceLocation(16, 18),
                SourceLocation(19, 35),
                SourceLocation(22, 35),
                SourceLocation(25, 35),
            )

        assertThat(errors[0]).hasMessage(ModifierClickableOrder.ModifierChainWithSuspiciousOrder)
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

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(4, 29),
                SourceLocation(7, 29),
                SourceLocation(10, 18),
                SourceLocation(13, 18),
                SourceLocation(16, 18),
            )

        assertThat(errors[0]).hasMessage(ModifierClickableOrder.ModifierChainWithSuspiciousOrder)
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

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
