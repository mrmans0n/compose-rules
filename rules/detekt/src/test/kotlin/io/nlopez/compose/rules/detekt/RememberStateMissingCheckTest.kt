// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import dev.detekt.test.lint
import io.nlopez.compose.rules.RememberStateMissing
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RememberStateMissingCheckTest {

    private val rule = RememberStateMissingCheck(Config.empty)

    @Test
    fun `passes when a non-remembered mutableStateOf is used outside of a Composable`() {
        @Language("kotlin")
        val code =
            """
                val msof = mutableStateOf("X")
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).hasSize(2)
            .hasStartSourceLocations(
                SourceLocation(3, 21),
                SourceLocation(6, 45),
            )
        for (error in errors) {
            assertThat(error).hasMessage(RememberStateMissing.errorMessage("mutableStateOf"))
        }
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when a non-remembered derivedStateOf is used outside of a Composable`() {
        @Language("kotlin")
        val code =
            """
                val dsof = derivedStateOf("X")
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).hasSize(2)
            .hasStartSourceLocations(
                SourceLocation(3, 21),
                SourceLocation(6, 45),
            )
        for (error in errors) {
            assertThat(error).hasMessage(RememberStateMissing.errorMessage("derivedStateOf"))
        }
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).hasSize(9)
            .hasStartSourceLocations(
                SourceLocation(3, 13),
                SourceLocation(4, 13),
                SourceLocation(5, 13),
                SourceLocation(6, 13),
                SourceLocation(7, 13),
                SourceLocation(8, 13),
                SourceLocation(9, 13),
                SourceLocation(10, 13),
                SourceLocation(11, 13),
            )
        // Just verify the first error message contains the expected pattern
        assertThat(errors.first()).hasMessage(RememberStateMissing.errorMessage("mutableIntListOf"))
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when a retain mutableStateOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable(
                    something: State<String> = retain { mutableStateOf("X") }
                ) {
                    val something = retain { mutableStateOf("X") }
                    val something2 by retain { mutableStateOf("Y") }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when a retain derivedStateOf is used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable(
                    something: State<String> = retain { derivedStateOf { "X" } }
                ) {
                    val something = retain { derivedStateOf { "X" } }
                    val something2 by retain { derivedStateOf { "Y" } }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes when retained collection-based mutableState functions are used in a Composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() {
                    val a = retain { mutableIntListOf() }
                    val b = retain { mutableLongListOf() }
                    val c = retain { mutableFloatListOf() }
                    val d = retain { mutableIntSetOf() }
                    val e = retain { mutableLongSetOf() }
                    val f = retain { mutableFloatSetOf() }
                    val g = retain { mutableIntIntMapOf() }
                    val h = retain { mutableLongLongMapOf() }
                    val i = retain { mutableFloatFloatMapOf() }
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
