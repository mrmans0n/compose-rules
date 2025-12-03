// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.RememberStateMissing
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RememberStateMissingCheckTest {

    private val rememberRuleAssertThat = assertThatRule { RememberStateMissingCheck() }

    @Test
    fun `passes when a non-remembered mutableStateOf is used outside of a Composable`() {
        @Language("kotlin")
        val code =
            """
                val msof = mutableStateOf("X")
            """.trimIndent()
        rememberRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when a non-remembered mutableStateOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val something = mutableStateOf("X")
                }
                @Composable
                fun MyComposable(something: State<String> = mutableStateOf("X")) {
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 21,
                detail = RememberStateMissing.errorMessage("mutableStateOf"),
            ),
            LintViolation(
                line = 6,
                col = 45,
                detail = RememberStateMissing.errorMessage("mutableStateOf"),
            ),
        )
    }

    @Test
    fun `passes when a remembered mutableStateOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable(
                    something: State<String> = remember { mutableStateOf("X") }
                ) {
                    val something = remember { mutableStateOf("X") }
                    val something2 by remember { mutableStateOf("Y") }
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when a rememberSaveable mutableStateOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable(
                    something: State<String> = rememberSaveable { mutableStateOf("X") }
                ) {
                    val something = rememberSaveable { mutableStateOf("X") }
                    val something2 by rememberSaveable { mutableStateOf("Y") }
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when a non-remembered derivedStateOf is used outside of a Composable`() {
        @Language("kotlin")
        val code =
            """
                val dsof = derivedStateOf("X")
            """.trimIndent()
        rememberRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when a non-remembered derivedStateOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val something = derivedStateOf { "X" }
                }
                @Composable
                fun MyComposable(something: State<String> = derivedStateOf { "X" }) {
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 21,
                detail = RememberStateMissing.errorMessage("derivedStateOf"),
            ),
            LintViolation(
                line = 6,
                col = 45,
                detail = RememberStateMissing.errorMessage("derivedStateOf"),
            ),
        )
    }

    @Test
    fun `passes when a remembered derivedStateOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable(
                    something: State<String> = remember { derivedStateOf { "X" } }
                ) {
                    val something = remember { derivedStateOf { "X" } }
                    val something2 by remember { derivedStateOf { "Y" } }
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when non-remembered collection-based mutableState functions are used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val a = mutableIntListOf()
                    val b = mutableLongListOf()
                    val c = mutableFloatListOf()
                    val d = mutableIntSetOf()
                    val e = mutableLongSetOf()
                    val f = mutableFloatSetOf()
                    val g = mutableIntIntMapOf()
                    val h = mutableLongLongMapOf()
                    val i = mutableFloatFloatMapOf()
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableIntListOf"),
            ),
            LintViolation(
                line = 4,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableLongListOf"),
            ),
            LintViolation(
                line = 5,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableFloatListOf"),
            ),
            LintViolation(
                line = 6,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableIntSetOf"),
            ),
            LintViolation(
                line = 7,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableLongSetOf"),
            ),
            LintViolation(
                line = 8,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableFloatSetOf"),
            ),
            LintViolation(
                line = 9,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableIntIntMapOf"),
            ),
            LintViolation(
                line = 10,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableLongLongMapOf"),
            ),
            LintViolation(
                line = 11,
                col = 13,
                detail = RememberStateMissing.errorMessage("mutableFloatFloatMapOf"),
            ),
        )
    }

    @Test
    fun `passes when remembered collection-based mutableState functions are used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val a = remember { mutableIntListOf() }
                    val b = remember { mutableLongListOf() }
                    val c = remember { mutableFloatListOf() }
                    val d = remember { mutableIntSetOf() }
                    val e = remember { mutableLongSetOf() }
                    val f = remember { mutableFloatSetOf() }
                    val g = remember { mutableIntIntMapOf() }
                    val h = remember { mutableLongLongMapOf() }
                    val i = remember { mutableFloatFloatMapOf() }
                }
            """.trimIndent()
        rememberRuleAssertThat(code).hasNoLintViolations()
    }
}
