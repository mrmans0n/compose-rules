// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.Material2
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class Material2CheckTest {

    private val ruleAssertThat = assertThatRule { Material2Check() }

    @Test
    fun `disabled by default`() {
        @Language("kotlin")
        val code =
            """
                import androidx.compose.material.Surface
            """.trimIndent()

        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when there is a M2 reference in imports`() {
        @Language("kotlin")
        val code =
            """
                import androidx.compose.material.Surface
                import androidx.compose.material.Text
                import androidx.compose.material.Typography as M2Typography
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(disallowMaterial2 to true)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 1,
                    col = 1,
                    detail = Material2.DisallowedUsageOfMaterial2,
                ),
                LintViolation(
                    line = 2,
                    col = 1,
                    detail = Material2.DisallowedUsageOfMaterial2,
                ),
                LintViolation(
                    line = 3,
                    col = 1,
                    detail = Material2.DisallowedUsageOfMaterial2,
                ),
            )
    }

    @Test
    fun `passes when there is a M2 reference in imports but it's in the allowlist`() {
        @Language("kotlin")
        val code =
            """
                import androidx.compose.material.icons.Icons
                import androidx.compose.material.icons.Icons.Arrow
                import androidx.compose.material.TopAppBar
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(
                disallowMaterial2 to true,
                allowedFromM2 to "icons.Icons,TopAppBar",
            )
            .hasNoLintViolations()
    }

    @Test
    fun `errors when there is a M2 reference in dot qualified expressions`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    androidx.compose.material.Text("hi")
                    Icon(imageVector = androidx.compose.material.icons.Icons.ArrowBack, contentDescription = null)
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(disallowMaterial2 to true)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 3,
                    col = 5,
                    detail = Material2.DisallowedUsageOfMaterial2,
                ),
                LintViolation(
                    line = 4,
                    col = 24,
                    detail = Material2.DisallowedUsageOfMaterial2,
                ),
            )
    }

    @Test
    fun `passes when there is a M2 reference in dot qualified expressions but it's in the allowlist`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    androidx.compose.material.TopAppBar(title = { Text("boo") })
                    Icon(imageVector = androidx.compose.material.icons.Icons.Arrow, contentDescription = null)
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(
                disallowMaterial2 to true,
                allowedFromM2 to "icons.Icons,TopAppBar",
            )
            .hasNoLintViolations()
    }
}
