// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import dev.detekt.test.lint
import io.nlopez.compose.rules.RememberContentMissing
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RememberContentMissingCheckTest {

    private val rule = RememberContentMissingCheck(Config.empty)

    @Test
    fun `passes when a non-remembered movableContentOf is used outside of a Composable`() {
        @Language("kotlin")
        val code =
            """
                val mco = movableContentOf { Text("X") }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors when a non-remembered movableContentOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val something = movableContentOf { Text("X") }
                }

            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(3, 21),
            )
        for (error in errors) {
            assertThat(error).hasMessage(RememberContentMissing.MovableContentOfNotRemembered)
        }
    }

    @Test
    fun `errors when a non-remembered movableContentWithReceiverOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val something = movableContentWithReceiverOf { Text("X") }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(3, 21),
            )
        for (error in errors) {
            assertThat(error).hasMessage(RememberContentMissing.MovableContentWithReceiverOfNotRemembered)
        }
    }
}
