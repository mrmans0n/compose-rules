// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MissingReadOnlyComposableCheckTest {

    private val rule = MissingReadOnlyComposableCheck(Config.empty)

    @Test
    fun `reports composable function that only calls read only composables`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun currentValue(): Int = 42

            @Composable
            fun Example(): Int = currentValue()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 9))
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    @Test
    fun `reports composable getter that only calls read only composables`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun currentValue(): Int = 42

            val value: Int
                @Composable
                get() = currentValue()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(9, 13))
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    @Test
    fun `reports composable function that reads read only composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val currentValue: Int
                @ReadOnlyComposable
                @Composable
                get() = 42

            @Composable
            fun Example(): Int = currentValue
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(9, 9))
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    @Test
    fun `reports composable function that only reads composition local current`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(): Int = LocalCount.current
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(6, 9))
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    @Test
    fun `does not report composition local current read with ordinary receiver call`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            fun localWithSideEffects(): CompositionLocal<Int> = compositionLocalOf { 0 }

            @Composable
            fun Example(): Int = localWithSideEffects().current
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition local current read with ordinary operator receiver call`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Locals {
                operator fun get(index: Int): CompositionLocal<Int> = compositionLocalOf { index }
            }

            @Composable
            fun Example(locals: Locals): Int = locals[0].current
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function without read only usage`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(): Int = 42
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function that calls non read only composable`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            @Composable
            fun Example() {
                EmitsContent()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function that calls composable lambda parameter`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(content: @Composable () -> Unit) {
                content()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function with read only usage and ordinary call`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            fun trackMetric() {
            }

            @Composable
            fun Example(): Int {
                trackMetric()
                return LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function with read only usage and ordinary custom getter read`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            val sideEffectingValue: Int
                get() = 1

            @Composable
            fun Example(): Int = LocalCount.current + sideEffectingValue
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function with read only usage and delegated property read`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            val token by lazy { 1 }

            @Composable
            fun Example(): Int = LocalCount.current + token
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function with read only usage and custom operator call`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCounter = compositionLocalOf { Counter(0) }

            class Counter(val value: Int) {
                operator fun plus(other: Counter): Counter = Counter(value + other.value)
            }

            @Composable
            fun Example(other: Counter): Counter = LocalCounter.current + other
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function with read only usage and mutation`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            var count = 0

            @Composable
            fun Example(): Int {
                count += 1
                return LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report already read only composable function`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun Example(): Int = 42
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report override composable function`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            abstract class Base {
                @Composable
                abstract fun currentValue(): Int
            }

            class Child : Base() {
                @Composable
                override fun currentValue(): Int = 42
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report override composable getter`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            abstract class Base {
                abstract val currentValue: Int
                    @Composable get
            }

            class Child : Base() {
                override val currentValue: Int
                    @Composable get() = LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable getter that calls non read only composable`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            val value: Unit
                @Composable
                get() = EmitsContent()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports read only usage inside eager stdlib lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun currentValue(): Int = 42

            @Composable
            fun Example(): Int = run {
                currentValue()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 9))
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    @Test
    fun `reports read only usage inside labeled eager stdlib lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun currentValue(): Int = 42

            @Composable
            fun Example(): Int = run label@{
                currentValue()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 9))
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    @Test
    fun `does not report read only usage inside deferred lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun currentValue(): Int = 42

            @Composable
            fun Example(): () -> Int = {
                currentValue()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report deferred lambda returned from eager stdlib lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun currentValue(): Int = 42

            @Composable
            fun Example(): () -> Int = run {
                {
                    currentValue()
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }
}
