// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.MutableStateParameter
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MutableStateParameterCheckTest {

    private val mutableParamRuleAssertThat = assertThatRule { MutableStateParameterCheck() }

    @Test
    fun `errors when a Composable has a mutable parameter`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Something(a: MutableState<String>) {}
            """.trimIndent()
        mutableParamRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 15,
                detail = MutableStateParameter.MutableStateParameterInCompose,
            ),
        )
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
        mutableParamRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 5,
                detail = MutableStateParameter.MutableStateParameterInCompose,
            ),
            LintViolation(
                line = 4,
                col = 5,
                detail = MutableStateParameter.MutableStateParameterInCompose,
            ),
            LintViolation(
                line = 5,
                col = 5,
                detail = MutableStateParameter.MutableStateParameterInCompose,
            ),
            LintViolation(
                line = 6,
                col = 5,
                detail = MutableStateParameter.MutableStateParameterInCompose,
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
                fun Something(a: State<String>) {}
            """.trimIndent()
        mutableParamRuleAssertThat(code).hasNoLintViolations()
    }
}
