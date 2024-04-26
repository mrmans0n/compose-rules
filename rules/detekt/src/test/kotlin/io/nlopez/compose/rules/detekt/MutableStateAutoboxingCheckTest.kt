// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.MutableStateAutoboxing
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MutableStateAutoboxingCheckTest {

    private val rule = MutableStateAutoboxingCheck(Config.empty)

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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(1, 10),
                SourceLocation(2, 10),
                SourceLocation(3, 10),
                SourceLocation(4, 10),
            )
        assertThat(errors[0]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingInt)
        assertThat(errors[1]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLong)
        assertThat(errors[2]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingDouble)
        assertThat(errors[3]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloat)
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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 14),
                SourceLocation(3, 14),
                SourceLocation(4, 14),
                SourceLocation(5, 14),
            )
        assertThat(errors[0]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingInt)
        assertThat(errors[1]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLong)
        assertThat(errors[2]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingDouble)
        assertThat(errors[3]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloat)
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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 14),
                SourceLocation(3, 14),
                SourceLocation(4, 14),
                SourceLocation(7, 14),
                SourceLocation(8, 14),
                SourceLocation(9, 14),
                SourceLocation(12, 14),
                SourceLocation(13, 14),
                SourceLocation(14, 14),
            )
        assertThat(errors[0]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntList)
        assertThat(errors[1]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongList)
        assertThat(errors[2]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatList)
        assertThat(errors[3]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntList)
        assertThat(errors[4]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongList)
        assertThat(errors[5]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatList)
        assertThat(errors[6]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntList)
        assertThat(errors[7]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongList)
        assertThat(errors[8]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatList)
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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 14),
                SourceLocation(3, 14),
                SourceLocation(4, 14),
                SourceLocation(7, 14),
                SourceLocation(8, 14),
                SourceLocation(9, 14),
                SourceLocation(12, 14),
                SourceLocation(13, 14),
                SourceLocation(14, 14),
            )
        assertThat(errors[0]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntSet)
        assertThat(errors[1]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongSet)
        assertThat(errors[2]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatSet)
        assertThat(errors[3]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntSet)
        assertThat(errors[4]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongSet)
        assertThat(errors[5]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatSet)
        assertThat(errors[6]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntSet)
        assertThat(errors[7]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongSet)
        assertThat(errors[8]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatSet)
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
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(2, 14),
                SourceLocation(3, 14),
                SourceLocation(4, 14),
                SourceLocation(7, 14),
                SourceLocation(8, 14),
                SourceLocation(9, 14),
                SourceLocation(12, 14),
                SourceLocation(13, 14),
                SourceLocation(14, 14),
            )
        assertThat(errors[0]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntIntMap)
        assertThat(errors[1]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntLongMap)
        assertThat(errors[2]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingIntFloatMap)
        assertThat(errors[3]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongIntMap)
        assertThat(errors[4]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongLongMap)
        assertThat(errors[5]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingLongFloatMap)
        assertThat(errors[6]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatIntMap)
        assertThat(errors[7]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatLongMap)
        assertThat(errors[8]).hasMessage(MutableStateAutoboxing.MutableStateAutoboxingFloatFloatMap)
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
