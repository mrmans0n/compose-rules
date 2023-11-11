// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.MutableParameters
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MutableParametersCheckTest {

    private val mutableParamRuleAssertThat = assertThatRule { MutableParametersCheck() }

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
        mutableParamRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 15,
                detail = MutableParameters.MutableParameterInCompose,
            ),
            LintViolation(
                line = 4,
                col = 15,
                detail = MutableParameters.MutableParameterInCompose,
            ),
            LintViolation(
                line = 6,
                col = 15,
                detail = MutableParameters.MutableParameterInCompose,
            ),
            LintViolation(
                line = 8,
                col = 15,
                detail = MutableParameters.MutableParameterInCompose,
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
