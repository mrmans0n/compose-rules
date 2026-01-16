// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.StateParameter
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class StateParameterCheckTest {

    private val stateParamRuleAssertThat = assertThatRule { StateParameterCheck() }

    @Test
    fun `errors when a Composable has a State parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(a: State<String>) {}
            """.trimIndent()
        stateParamRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 15,
                detail = StateParameter.StateParameterInCompose,
            ),
        )
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
        stateParamRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 5,
                detail = StateParameter.StateParameterInCompose,
            ),
            LintViolation(
                line = 4,
                col = 5,
                detail = StateParameter.StateParameterInCompose,
            ),
            LintViolation(
                line = 5,
                col = 5,
                detail = StateParameter.StateParameterInCompose,
            ),
            LintViolation(
                line = 6,
                col = 5,
                detail = StateParameter.StateParameterInCompose,
            ),
        )
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
        stateParamRuleAssertThat(code).hasNoLintViolations()
    }
}
