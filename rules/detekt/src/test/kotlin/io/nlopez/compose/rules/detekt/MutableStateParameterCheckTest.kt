// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.MutableStateParameter
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MutableStateParameterCheckTest {

    private val rule = MutableStateParameterCheck(Config.empty)

    @Test
    fun `errors when a Composable has a MutableState parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(a: MutableState<String>) {}
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 15),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MutableStateParameter.MutableStateParameterInCompose)
        }
    }

    @Test
    fun `errors when a Composable has primitive MutableState parameters`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(
                    a: MutableIntState,
                    b: MutableFloatState,
                    c: MutableDoubleState,
                    d: MutableLongState
                ) {}
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(3, 5),
                SourceLocation(4, 5),
                SourceLocation(5, 5),
                SourceLocation(6, 5),
            )
        for (error in errors) {
            assertThat(error).hasMessage(MutableStateParameter.MutableStateParameterInCompose)
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
