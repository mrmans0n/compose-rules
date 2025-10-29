// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.nlopez.compose.rules.ModifierComposed
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierComposedCheckTest {

    private val rule = ModifierComposedCheck(Config.empty)

    @Test
    fun `errors when a composed Modifier extension is detected`() {
        @Language("kotlin")
        val code =
            """
                fun Modifier.something1(): Modifier = composed {}
                fun Modifier.something2() = composed {}
                fun Modifier.something3() {
                    return composed {}
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasTextLocations("something1", "something2", "something3")
        for (error in errors) {
            assertThat(error).hasMessage(ModifierComposed.ComposedModifier)
        }
    }
}
