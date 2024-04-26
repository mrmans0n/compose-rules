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
    fun `errors when a mutableStateOf for a numeric list parameter is used`() {
        @Language("kotlin")
        val code =
            """
                fun myFunction(a: List<Int>, b: List<Long>, c: List<Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
                fun myFunction(a: ImmutableList<Int>, b: ImmutableList<Long>, c: ImmutableList<Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
                fun myFunction(a: PersistentList<Int>, b: PersistentList<Long>, c: PersistentList<Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
            """.trimIndent()
        ruleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntList,
                ),
                LintViolation(
                    line = 3,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongList,
                ),
                LintViolation(
                    line = 4,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatList,
                ),
                LintViolation(
                    line = 7,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntList,
                ),
                LintViolation(
                    line = 8,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongList,
                ),
                LintViolation(
                    line = 9,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatList,
                ),
                LintViolation(
                    line = 12,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntList,
                ),
                LintViolation(
                    line = 13,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongList,
                ),
                LintViolation(
                    line = 14,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatList,
                ),
            )
    }

    @Test
    fun `errors when a mutableStateOf for a numeric set parameter is used`() {
        @Language("kotlin")
        val code =
            """
                fun myFunction(a: Set<Int>, b: Set<Long>, c: Set<Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
                fun myFunction(a: ImmutableSet<Int>, b: ImmutableSet<Long>, c: ImmutableSet<Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
                fun myFunction(a: PersistentSet<Int>, b: PersistentSet<Long>, c: PersistentSet<Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
            """.trimIndent()
        ruleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntSet,
                ),
                LintViolation(
                    line = 3,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongSet,
                ),
                LintViolation(
                    line = 4,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatSet,
                ),
                LintViolation(
                    line = 7,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntSet,
                ),
                LintViolation(
                    line = 8,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongSet,
                ),
                LintViolation(
                    line = 9,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatSet,
                ),
                LintViolation(
                    line = 12,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntSet,
                ),
                LintViolation(
                    line = 13,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongSet,
                ),
                LintViolation(
                    line = 14,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatSet,
                ),
            )
    }

    @Test
    fun `errors when a mutableStateOf for a numeric map parameter pair is used`() {
        @Language("kotlin")
        val code =
            """
                fun myFunction(a: Map<Int, Int>, b: Map<Int, Long>, c: Map<Int, Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
                fun myFunction(a: Map<Long, Int>, b: Map<Long, Long>, c: Map<Long, Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
                fun myFunction(a: Map<Float, Int>, b: Map<Float, Long>, c: Map<Float, Float>) {
                    var a by mutableStateOf(a)
                    var b by mutableStateOf(b)
                    var c by mutableStateOf(c)
                }
            """.trimIndent()
        ruleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntIntMap,
                ),
                LintViolation(
                    line = 3,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntLongMap,
                ),
                LintViolation(
                    line = 4,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingIntFloatMap,
                ),
                LintViolation(
                    line = 7,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongIntMap,
                ),
                LintViolation(
                    line = 8,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongLongMap,
                ),
                LintViolation(
                    line = 9,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingLongFloatMap,
                ),
                LintViolation(
                    line = 12,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatIntMap,
                ),
                LintViolation(
                    line = 13,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatLongMap,
                ),
                LintViolation(
                    line = 14,
                    col = 14,
                    detail = MutableStateAutoboxing.MutableStateAutoboxingFloatFloatMap,
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
