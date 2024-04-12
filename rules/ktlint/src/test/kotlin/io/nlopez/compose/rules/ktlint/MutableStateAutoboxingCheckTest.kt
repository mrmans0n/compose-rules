// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.MutableStateAutoboxing
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MutableStateAutoboxingCheckTest {

    private val ruleAssertThat = assertThatRule { MutableStateAutoboxingCheck() }

    @Test
    fun `errors when a mutableStateOf for a numeric constant is used`() {
        @Language("kotlin")
        val code =
            """
                var a by mutableStateOf(0)
                var b by mutableStateOf(0L)
                var c by mutableStateOf(0.0)
                var d by mutableStateOf(0f)
            """.trimIndent()

        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 1,
                col = 10,
                detail = MutableStateAutoboxing.MutableStateAutoboxingInt,
            ),
            LintViolation(
                line = 2,
                col = 10,
                detail = MutableStateAutoboxing.MutableStateAutoboxingLong,
            ),
            LintViolation(
                line = 3,
                col = 10,
                detail = MutableStateAutoboxing.MutableStateAutoboxingDouble,
            ),
            LintViolation(
                line = 4,
                col = 10,
                detail = MutableStateAutoboxing.MutableStateAutoboxingFloat,
            ),
        )
    }

    @Test
    fun `errors when a mutableStateOf for a numeric parameter is used`() {
        @Language("kotlin")
        val code =
            """
                fun myFunction(a: Int, b: Long, c: Double, d: Float) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                    var d by mutableStateOf(d)
                }
            """.trimIndent()
        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 14,
                detail = MutableStateAutoboxing.MutableStateAutoboxingInt,
            ),
            LintViolation(
                line = 3,
                col = 14,
                detail = MutableStateAutoboxing.MutableStateAutoboxingLong,
            ),
            LintViolation(
                line = 4,
                col = 14,
                detail = MutableStateAutoboxing.MutableStateAutoboxingDouble,
            ),
            LintViolation(
                line = 5,
                col = 14,
                detail = MutableStateAutoboxing.MutableStateAutoboxingFloat,
            ),
        )
    }

    @Test
    fun `no errors when a mutableStateOf is used for other things`() {
        @Language("kotlin")
        val code =
            """
                var a by mutableStateOf("")
                var b by mutableStateOf(true)
                fun bleh(c: String) {
                    var ccc by mutableStateOf(c)
                }
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }
}
