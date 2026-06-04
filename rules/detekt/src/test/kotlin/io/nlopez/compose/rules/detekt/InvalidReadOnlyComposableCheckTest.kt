// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class InvalidReadOnlyComposableCheckTest {

    private val rule = InvalidReadOnlyComposableCheck(Config.empty)

    @Test
    fun `reports read only composable function that calls non read only composable`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            @ReadOnlyComposable
            @Composable
            fun Example(): Int {
                EmitsContent()
                return 42
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(11, 13))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `reports read only composable getter that calls non read only composable`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun currentValue(): Int = 42

            val value: Int
                @ReadOnlyComposable
                @Composable
                get() = currentValue()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(10, 21))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `reports read only composable function that reads non read only composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val emitsContent: Unit
                @Composable
                get() {
                }

            @ReadOnlyComposable
            @Composable
            fun Example() {
                emitsContent
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(12, 13))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `reports read only composable function that reads qualified non read only composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            object Content {
                val emitsContent: Unit
                    @Composable
                    get() {
                    }
            }

            @ReadOnlyComposable
            @Composable
            fun Example() {
                Content.emitsContent
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(14, 21))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `does not report read only composable function that reads read only composable property`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val currentValue: Int
                @ReadOnlyComposable
                @Composable
                get() = 42

            @ReadOnlyComposable
            @Composable
            fun Example(): Int = currentValue
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports read only composable function that calls composable lambda parameter`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun Example(content: @Composable () -> Unit) {
                content()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(7, 13))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `does not report read only composable function that only calls read only composables`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @ReadOnlyComposable
            @Composable
            fun localContext(): String = "context"

            @ReadOnlyComposable
            @Composable
            fun Example(): String = localContext()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report composable function without read only annotation`() {
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
    fun `does not report read only composable function without composable calls`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            fun currentValue(): Int = 42

            @ReadOnlyComposable
            @Composable
            fun Example(): Int = currentValue()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report non read only composable call inside deferred lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            @ReadOnlyComposable
            @Composable
            fun Example(): () -> Unit = {
                EmitsContent()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports non read only composable call inside eager stdlib scope lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            @ReadOnlyComposable
            @Composable
            fun Example(): Unit = run {
                EmitsContent()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(11, 13))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `reports non read only composable call inside labeled eager stdlib scope lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            @ReadOnlyComposable
            @Composable
            fun Example(): Unit = run label@{
                EmitsContent()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(11, 13))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `reports non read only composable call inside repeat lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            @ReadOnlyComposable
            @Composable
            fun Example() {
                repeat(1) {
                    EmitsContent()
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(12, 17))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `reports non read only composable call inside collection forEach lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            @ReadOnlyComposable
            @Composable
            fun Example(items: List<Int>) {
                items.forEach {
                    EmitsContent()
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(12, 17))
            .hasMessage(InvalidReadOnlyComposableCheck.InvalidReadOnlyComposable)
    }

    @Test
    fun `does not report non read only composable call inside custom deferred run lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            class DeferredRunner {
                fun run(block: () -> Unit) = block
            }

            @ReadOnlyComposable
            @Composable
            fun Example(runner: DeferredRunner): () -> Unit = runner.run {
                EmitsContent()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report non read only composable call inside custom deferred forEach lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun EmitsContent() {
            }

            class DeferredItems {
                fun forEach(block: () -> Unit) = block
            }

            @ReadOnlyComposable
            @Composable
            fun Example(items: DeferredItems): () -> Unit = items.forEach {
                EmitsContent()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }
}
