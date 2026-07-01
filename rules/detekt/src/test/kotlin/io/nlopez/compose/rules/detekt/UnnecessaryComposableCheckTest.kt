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
    fun `does not report composition use inside eager collection association`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            enum class Month {
                January,
            }

            @Composable
            fun Month.shortName(): String = LocalMonthName.current

            val LocalMonthName = compositionLocalOf { "Jan" }

            @Composable
            fun Example(): Map<Month, String> = Month.entries.associateWith { month ->
                month.shortName()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager sequence association`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(items: Sequence<Int>): Map<Int, Int> = items.associateWith { item ->
                item + LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager collection association variants`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun AssociateExample(items: List<Int>): Map<Int, Int> = items.associate { item ->
                item to LocalCount.current
            }

            @Composable
            fun AssociateByExample(items: List<Int>): Map<Int, Int> = items.associateBy { item ->
                item + LocalCount.current
            }

            @Composable
            fun GroupByExample(items: List<Int>): Map<Int, List<Int>> = items.groupBy { item ->
                item + LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager collection transform variants`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun PartitionExample(items: List<Int>): Pair<List<Int>, List<Int>> = items.partition { item ->
                item > LocalCount.current
            }

            @Composable
            fun MapIndexedNotNullExample(items: List<Int>): List<Int> = items.mapIndexedNotNull { index, item ->
                index + item + LocalCount.current
            }

            @Composable
            fun ZipExample(items: List<Int>, other: List<Int>): List<Int> = items.zip(other) { left, right ->
                left + right + LocalCount.current
            }

            @Composable
            fun ChunkedExample(items: List<Int>): List<Int> = items.chunked(2) { chunk ->
                chunk.sum() + LocalCount.current
            }

            @Composable
            fun WindowedExample(items: List<Int>): List<Int> = items.windowed(2) { window ->
                window.sum() + LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager sequence terminal operations`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun AssociateExample(items: Sequence<Int>): Map<Int, Int> = items.associate { item ->
                item to LocalCount.current
            }

            @Composable
            fun AssociateByExample(items: Sequence<Int>): Map<Int, Int> = items.associateBy { item ->
                item + LocalCount.current
            }

            @Composable
            fun GroupByExample(items: Sequence<Int>): Map<Int, List<Int>> = items.groupBy { item ->
                item + LocalCount.current
            }

            @Composable
            fun FoldExample(items: Sequence<Int>): Int = items.fold(0) { acc, item ->
                acc + item + LocalCount.current
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports composable function when composition use is inside lazy sequence transform`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalCount = compositionLocalOf { 0 }

            @Composable
            fun Example(items: Sequence<Int>): Sequence<Int> = items.map { item ->
                item + LocalCount.current
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
    fun `does not report composition use inside eager stdlib builders`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val LocalText = compositionLocalOf { "text" }

            @Composable
            fun BuildStringExample(): String = buildString {
                append(LocalText.current)
            }

            @Composable
            fun BuildListExample(): List<String> = buildList {
                add(LocalText.current)
            }

            @Composable
            fun BuildSetExample(): Set<String> = buildSet {
                add(LocalText.current)
            }

            @Composable
            fun BuildMapExample(): Map<String, String> = buildMap {
                put(LocalText.current, LocalText.current)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager builder lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            import androidx.compose.ui.text.buildAnnotatedString
            import androidx.compose.ui.text.AnnotatedString

            val LocalText = compositionLocalOf { "text" }

            @Composable
            fun Example(): AnnotatedString = buildAnnotatedString {
                append(LocalText.current)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(
            code,
            """
            package androidx.compose.ui.text

            class AnnotatedString

            class AnnotatedStringBuilder {
                fun append(value: String) = Unit
            }

            fun buildAnnotatedString(builder: AnnotatedStringBuilder.() -> Unit): AnnotatedString {
                AnnotatedStringBuilder().builder()
                return AnnotatedString()
            }
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composition use inside eager annotated string helper lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            import androidx.compose.ui.text.AnnotatedString
            import androidx.compose.ui.text.LinkAnnotation
            import androidx.compose.ui.text.SpanStyle
            import androidx.compose.ui.text.buildAnnotatedString
            import androidx.compose.ui.text.withAnnotation
            import androidx.compose.ui.text.withLink
            import androidx.compose.ui.text.withStyle

            val LocalText = compositionLocalOf { "text" }

            @Composable
            fun StyleExample(style: SpanStyle): AnnotatedString = buildAnnotatedString {
                withStyle(style) {
                    append(LocalText.current)
                }
            }

            @Composable
            fun LinkExample(link: LinkAnnotation): AnnotatedString = buildAnnotatedString {
                withLink(link) {
                    append(LocalText.current)
                }
            }

            @Composable
            fun AnnotationExample(): AnnotatedString = buildAnnotatedString {
                withAnnotation("tag", "annotation") {
                    append(LocalText.current)
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(
            code,
            """
            package androidx.compose.ui.text

            class AnnotatedString

            class LinkAnnotation

            class SpanStyle

            class AnnotatedStringBuilder {
                fun append(value: String) = Unit
            }

            fun buildAnnotatedString(builder: AnnotatedStringBuilder.() -> Unit): AnnotatedString {
                AnnotatedStringBuilder().builder()
                return AnnotatedString()
            }

            fun AnnotatedStringBuilder.withStyle(style: SpanStyle, block: AnnotatedStringBuilder.() -> Unit) {
                block()
            }

            fun AnnotatedStringBuilder.withLink(link: LinkAnnotation, block: AnnotatedStringBuilder.() -> Unit) {
                block()
            }

            fun AnnotatedStringBuilder.withAnnotation(
                tag: String,
                annotation: String,
                block: AnnotatedStringBuilder.() -> Unit,
            ) {
                block()
            }
            """.trimIndent(),
        )

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

    @Test
    fun `does not report composable function that reads chained composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Palette(val primary: Int)

            val LocalPalette = compositionLocalOf { Palette(primary = 0) }

            val CurrentPalette: Palette
                @Composable
                get() = LocalPalette.current

            fun describe(value: Int): String = value.toString()

            @Composable
            fun Example(): String = describe(CurrentPalette.primary)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }
}
