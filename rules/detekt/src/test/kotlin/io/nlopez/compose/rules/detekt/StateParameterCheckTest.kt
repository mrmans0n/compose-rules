// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import dev.detekt.test.lint
import io.nlopez.compose.rules.StateParameter
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class StateParameterCheckTest {

    private val rule = StateParameterCheck(Config.empty)

    @Test
    fun `errors when a Composable has a State parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(a: State<String>) {}
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 15),
            )
        for (error in errors) {
            assertThat(error).hasMessage(StateParameter.StateParameterInCompose)
        }
    }

    @Test
    fun `errors when a Composable has primitive State parameters`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(
                    a: IntState,
                    b: FloatState,
                    c: DoubleState,
                    d: LongState
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
            assertThat(error).hasMessage(StateParameter.StateParameterInCompose)
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
                fun Something(a: MutableState<String>) {}
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
