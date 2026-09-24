// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Finding
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
    fun `reports composable function that reads chained read only composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val currentValue: Pair<Int, Int>
                @ReadOnlyComposable
                @Composable
                get() = 1 to 2

            @Composable
            fun Example(): Int = currentValue.first
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(9, 9))
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    @Test
    fun `does not report composable function that reads chained non read only composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val currentValue: Pair<Int, Int>
                @Composable
                get() = 1 to 2

            @Composable
            fun Example(): Int = currentValue.first
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
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

    @Test
    fun `reports read only usage with library call on the result`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(): String = stringResource(1).uppercase()
            """,
        )

        assertSingleFinding(rule.lintWithAnalysisApi(code), SourceLocation(10, 9))
    }

    @Test
    fun `reports read only usage inside let with library call on the result`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(value: String?): String = value?.let { stringResource(1) }.orEmpty()
            """,
        )

        assertSingleFinding(rule.lintWithAnalysisApi(code), SourceLocation(10, 9))
    }

    @Test
    fun `reports read only usage inside annotated string builder`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            import androidx.compose.ui.text.AnnotatedString
            import androidx.compose.ui.text.SpanStyle
            import androidx.compose.ui.text.buildAnnotatedString
            import androidx.compose.ui.text.withStyle

            $STRING_RESOURCE

            @Composable
            fun Example(style: SpanStyle): AnnotatedString {
                val text = stringResource(1)
                return buildAnnotatedString {
                    withStyle(style) {
                        append(text)
                    }
                }
            }
            """,
        )

        assertSingleFinding(rule.lintWithAnalysisApi(code, FAKE_COMPOSE_UI_TEXT), SourceLocation(15, 9))
    }

    @Test
    fun `reports read only usage with elvis operator`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(value: String?): String = value ?: stringResource(1)
            """,
        )

        assertSingleFinding(rule.lintWithAnalysisApi(code), SourceLocation(10, 9))
    }

    @Test
    fun `reports read only usage with library binary operators`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(count: Int, other: String?): String =
                if (count > 0 && other != null) stringResource(1) + count else stringResource(2)
            """,
        )

        assertSingleFinding(rule.lintWithAnalysisApi(code), SourceLocation(10, 9))
    }

    @Test
    fun `reports read only usage inside fast collection helper lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            import androidx.compose.ui.util.fastMap

            $STRING_RESOURCE

            @Composable
            fun Example(ids: List<Int>): String = ids.fastMap { stringResource(it) }.joinToString()
            """,
        )

        assertSingleFinding(rule.lintWithAnalysisApi(code, FAKE_COMPOSE_UI_UTIL), SourceLocation(12, 9))
    }

    @Test
    fun `reports annotated string assembled from read only usages`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            import androidx.compose.ui.text.AnnotatedString
            import androidx.compose.ui.text.buildAnnotatedString

            $STRING_RESOURCE

            class Price(val formattedValue: String)

            class PriceInfo(val reference: Price?, val perUnit: Price?, val showReferencePrice: Boolean)

            @Composable
            fun Example(price: PriceInfo, savings: String?): AnnotatedString {
                val referencePrice = stringResource(1, price.reference?.formattedValue.orEmpty())
                val perUnit = price.perUnit?.formattedValue?.let { stringResource(2, it) }
                return buildAnnotatedString {
                    if (price.showReferencePrice) {
                        append(referencePrice)
                        append(", ")
                    }
                    savings?.let {
                        append(it)
                        append(", ")
                    }
                    perUnit?.let { append(it) }
                }
            }
            """,
        )

        assertSingleFinding(rule.lintWithAnalysisApi(code, FAKE_COMPOSE_UI_TEXT), SourceLocation(17, 9))
    }

    @Test
    fun `does not report read only usage with non read only composable call`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Text(text: String) {
            }

            @Composable
            fun Example() {
                Text(stringResource(1))
            }
            """,
        )

        assertThat(rule.lintWithAnalysisApi(code)).isEmpty()
    }

    @Test
    fun `does not report read only usage with remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(): String {
                val prefix = remember { "prefix" }
                return prefix + stringResource(1)
            }
            """,
        )

        assertThat(rule.lintWithAnalysisApi(code)).isEmpty()
    }

    @Test
    fun `does not report read only usage inside non inline library lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(): Pair<String, Lazy<Int>> = stringResource(1) to lazy { 1 }
            """,
        )

        assertThat(rule.lintWithAnalysisApi(code)).isEmpty()
    }

    @Test
    fun `does not report read only usage with function type invocation`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(onRead: () -> Unit): String {
                onRead()
                return stringResource(1)
            }
            """,
        )

        assertThat(rule.lintWithAnalysisApi(code)).isEmpty()
    }

    @Test
    fun `does not report read only usage with local var mutation`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            @Composable
            fun Example(): String {
                var text = stringResource(1)
                text = text.trim()
                return text
            }
            """,
        )

        assertThat(rule.lintWithAnalysisApi(code)).isEmpty()
    }

    @Test
    fun `does not report read only usage with non read only composable operator`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            $STRING_RESOURCE

            class Label(val text: String)

            @Composable
            operator fun Label.plus(other: String): Label = Label(text + other)

            @Composable
            fun Example(label: Label): Label = label + stringResource(1)
            """,
        )

        assertThat(rule.lintWithAnalysisApi(code)).isEmpty()
    }

    private fun assertSingleFinding(findings: List<Finding>, location: SourceLocation) {
        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(location)
            .hasMessage(MissingReadOnlyComposableCheck.MissingReadOnlyComposable)
    }

    private companion object {
        const val STRING_RESOURCE = """
            @ReadOnlyComposable
            @Composable
            fun stringResource(id: Int, vararg formatArgs: Any): String = id.toString()
        """

        /** The builder is a library type, like the real `AnnotatedString.Builder`, so `append` resolves as library code. */
        const val FAKE_COMPOSE_UI_TEXT = """
            package androidx.compose.ui.text

            class AnnotatedString(val text: String)

            class SpanStyle

            typealias AnnotatedStringBuilder = StringBuilder

            inline fun buildAnnotatedString(builder: AnnotatedStringBuilder.() -> Unit): AnnotatedString =
                AnnotatedString(StringBuilder().apply(builder).toString())

            inline fun <R : Any> AnnotatedStringBuilder.withStyle(style: SpanStyle, block: AnnotatedStringBuilder.() -> R): R =
                block()
        """

        const val FAKE_COMPOSE_UI_UTIL = """
            package androidx.compose.ui.util

            inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> = map(transform)
        """
    }
}
