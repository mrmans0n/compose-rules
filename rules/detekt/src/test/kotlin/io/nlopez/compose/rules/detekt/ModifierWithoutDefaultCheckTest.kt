// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import io.nlopez.compose.rules.ModifierWithoutDefault
import io.nlopez.compose.rules.detekt.assertThat
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.jupiter.api.Test

class ModifierWithoutDefaultCheckTest {

    private val rule = ModifierWithoutDefaultCheck(Config.empty)

    @Test
    fun `errors when a Composable has modifiers but without default values`() {
        @Language("kotlin")
        val composableCode = """
                @Composable
                fun Something(modifier: Modifier) { }
                @Composable
                fun Something(modifier: Modifier = Modifier, modifier2: Modifier) { }
        """.trimIndent()

        val errors = rule.lint(composableCode)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(2, 15),
            SourceLocation(4, 46),
        )
        assertThat(errors[0]).hasMessage(ModifierWithoutDefault.MissingModifierDefaultParam)
        assertThat(errors[1]).hasMessage(ModifierWithoutDefault.MissingModifierDefaultParam)
    }

    @Test
    fun `autocorrects by adding a default value that keeps the Modifier type reference intact`() {
        @Language("kotlin")
        val composableCode = """
                @Composable
                fun Something(modifier: Modifier, title: String) { }
        """.trimIndent()

        val file = compileContentForTest(composableCode)
        ModifierWithoutDefaultCheck(TestConfig("autoCorrect" to true)).lint(file)

        assertThat(file.text).isEqualTo(
            """
            @Composable
            fun Something(modifier: Modifier = Modifier, title: String) { }
            """.trimIndent(),
        )

        // The type reference has to survive the fix as a real reference, or rules that resolve references
        // (such as unused import detection) would no longer see the Modifier import as used.
        val modifierParameter = file.collectDescendantsOfType<KtParameter>().first()
        assertThat(modifierParameter.typeReference?.text).isEqualTo("Modifier")
        assertThat(modifierParameter.defaultValue?.text).isEqualTo("Modifier")
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

        val errors = rule.lint(composableCode)
        assertThat(errors).isEmpty()
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

        val errors = rule.lint(composableCode)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
