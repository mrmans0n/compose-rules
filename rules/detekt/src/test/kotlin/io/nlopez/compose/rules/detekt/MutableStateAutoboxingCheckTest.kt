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
