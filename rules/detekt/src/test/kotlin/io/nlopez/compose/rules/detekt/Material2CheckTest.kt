// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.Material2
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class Material2CheckTest {

    private val testConfig = TestConfig(
        "allowedFromM2" to listOf("icons.Icons", "TopAppBar"),
    )
    private val rule = Material2Check(testConfig)

    @Test
    fun `errors when there is a M2 reference in imports`() {
        @Language("kotlin")
        val code =
            """
                import androidx.compose.material.Surface
                import androidx.compose.material.Text
                import androidx.compose.material.Typography as M2Typography
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(1, 1),
                SourceLocation(2, 1),
                SourceLocation(3, 1),
            )
        for (error in errors) {
            assertThat(error).hasMessage(Material2.DisallowedUsageOfMaterial2)
        }
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

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors when there is a M2 reference in dot qualified expressions`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something() {
                    androidx.compose.material.Text("hi")
                    Icon(imageVector = androidx.compose.material.icons.filled.ArrowBack, contentDescription = null)
                }
            """.trimIndent()

        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(3, 5),
                SourceLocation(4, 24),
            )
        for (error in errors) {
            assertThat(error).hasMessage(Material2.DisallowedUsageOfMaterial2)
        }
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

        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
