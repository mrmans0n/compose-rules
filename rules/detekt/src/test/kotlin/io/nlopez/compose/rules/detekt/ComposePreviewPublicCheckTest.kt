// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ComposePreviewPublic
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposePreviewPublicCheckTest {

    private val rule = ComposePreviewPublicCheck(Config.empty)

    @Test
    fun `passes for non-preview public composables`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors for preview public composables`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            fun MyComposable() { }
            @CombinedPreviews
            @Composable
            fun MyComposable() { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(3, 5),
            SourceLocation(6, 5),
        )
        for (error in errors) {
            assertThat(error).hasMessage(ComposePreviewPublic.ComposablesPreviewShouldNotBePublic)
        }
    }

    @Test
    fun `passes when a non-public preview composable uses preview params`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Composable
            private fun MyComposable(user: User) {
            }
            @CombinedPreviews
            @Composable
            internal fun MyComposable(user: User) {
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
