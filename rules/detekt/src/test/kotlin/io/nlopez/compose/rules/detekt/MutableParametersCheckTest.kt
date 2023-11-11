// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.MutableParameters
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MutableParametersCheckTest {

    private val rule = MutableParametersCheck(Config.empty)

    @Test
    fun `errors when a Composable has a mutable parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(a: MutableState<String>) {}
                @Composable
                fun Something(a: ArrayList<String>) {}
                @Composable
                fun Something(a: HashSet<String>) {}
                @Composable
                fun Something(a: MutableMap<String, String>) {}
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasSize(4)
            .hasStartSourceLocations(
                SourceLocation(2, 15),
                SourceLocation(4, 15),
                SourceLocation(6, 15),
                SourceLocation(8, 15),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MutableParameters.MutableParameterInCompose)
        }
    }

    @Test
    fun `no errors when a Composable has valid parameters`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(a: String, b: (Int) -> Unit) {}
                @Composable
                fun Something(a: State<String>) {}
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
