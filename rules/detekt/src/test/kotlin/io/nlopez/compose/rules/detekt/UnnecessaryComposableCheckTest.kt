// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class UnnecessaryComposableCheckTest {

    private val rule = UnnecessaryComposableCheck(Config.empty)

    @Test
    fun `reports composable function that does not use composition`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(): Int = 42
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(4, 9))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `reports composable getter that does not use composition`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val value: Int
                @Composable
                get() = 42
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(5, 13))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `does not report composable function that calls composable function`() {
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
    fun `does not report composable function that reads composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            val currentValue: Int
                @Composable
                get() = LocalCount.current

            @Composable
            fun Example(): Int = currentValue
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function that reads composition local current`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(): Int = LocalCount.current
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function that reads compose state value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: State<Int>): Int = state.value
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function that reads delegated compose state`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: State<Int>): Int {
                val count by state
                return count
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports composable function that only writes compose mutable state value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: MutableState<Int>) {
                state.value = 5
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(4, 9))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `does not report composable function that compound assigns compose mutable state value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: MutableState<Int>) {
                state.value += 1
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports composable function that only writes delegated compose mutable state`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: MutableState<Int>) {
                var count by state
                count = 5
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(4, 9))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `reports composable function that only writes qualified delegated compose mutable state`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Holder(state: MutableState<Int>) {
                var count by state

                @Composable
                fun Example() {
                    this.count = 5
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(7, 13))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `does not report composable function that reads nullable compose state value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: State<Int>?): Int? = state?.value
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function that reads primitive compose state values`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun IntExample(state: IntState): Int = state.intValue

            @Composable
            fun LongExample(state: LongState): Long = state.longValue

            @Composable
            fun FloatExample(state: FloatState): Float = state.floatValue

            @Composable
            fun DoubleExample(state: DoubleState): Double = state.doubleValue
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports composable function that only writes primitive compose mutable state value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: MutableIntState) {
                state.intValue = 5
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(4, 9))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `does not report composable function that compound assigns primitive compose mutable state value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: MutableIntState) {
                state.intValue += 1
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function that uses composition in default parameter value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(value: Int = LocalCount.current): Int = value
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function with composable slot parameter`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Wrapper(content: @Composable () -> Unit) {
                println(content)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function with typealiased composable slot parameter`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            typealias Slot = @Composable () -> Unit

            @Composable
            fun Wrapper(content: Slot) {
                println(content)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable extension function with composable receiver slot`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun (@Composable () -> Unit).Wrapper() {
                println(this)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report contract declarations`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            interface Screen {
                @Composable
                fun Content()
            }

            abstract class Base {
                @Composable
                open fun Render() = Unit

                @Composable
                abstract fun AbstractRender()
            }

            class Child : Base() {
                @Composable
                override fun Render() = Unit
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report read only composable declarations`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun Example(): Int = 42

            val value: Int
                @ReadOnlyComposable
                @Composable
                get() = 42
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager stdlib lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(): Int = run {
                LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager collection transform`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(items: List<Int>): List<Int> = items.map {
                it + LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside indexed eager collection iteration`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(items: List<Int>) = items.forEachIndexed { index, item ->
                println(index + item + LocalCount.current)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside custom inline lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            inline fun <T> immediate(block: () -> T): T = block()

            @Composable
            fun Example(): Int = immediate {
                LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside wrapped custom inline lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Target(AnnotationTarget.EXPRESSION)
            annotation class Marker

            val LocalCount = compositionLocalOf { 0 }

            inline fun <T> immediate(block: () -> T): T = block()

            @Composable
            fun LabeledExample(): Int = immediate(label@ {
                LocalCount.current
            })

            @Composable
            fun ParenthesizedExample(): Int = immediate(({
                LocalCount.current
            }))

            @Composable
            fun AnnotatedExample(): Int = immediate(@Marker {
                LocalCount.current
            })
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when inline noinline lambda only defers composition use`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            inline fun later(noinline block: () -> Int): () -> Int = block

            @Composable
            fun Example(state: State<Int>): () -> Int = later {
                state.value
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(6, 9))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `reports when inline crossinline lambda only defers composition use`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            inline fun later(crossinline block: () -> Int): () -> Int = { block() }

            @Composable
            fun Example(state: State<Int>): () -> Int = later {
                state.value
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(6, 9))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }

    @Test
    fun `does not report composition use inside eager collection reducers`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun FoldExample(items: List<Int>): Int = items.fold(0) { acc, item ->
                acc + item + LocalCount.current
            }

            @Composable
            fun ReduceExample(items: List<Int>): Int = items.reduce { acc, item ->
                acc + item + LocalCount.current
            }

            @Composable
            fun OnEachExample(items: List<Int>): List<Int> = items.onEach {
                LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports composable function when composition use is only inside deferred lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(): () -> Int = {
                LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(6, 9))
            .hasMessage(UnnecessaryComposableCheck.UnnecessaryComposable)
    }
}
