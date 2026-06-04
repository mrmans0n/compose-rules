// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class VarsWithoutStateBackingCheckTest {

    private val rule = VarsWithoutStateBackingCheck(Config.empty)

    @Test
    fun `reports bare local vars in composable functions`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example() {
                var count = 0
                if (count == 0) {
                    var label = "empty"
                    println(label)
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(2)
        assertThat(findings[0])
            .hasStartSourceLocation(SourceLocation(6, 13))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
        assertThat(findings[1])
            .hasStartSourceLocation(SourceLocation(8, 17))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
    }

    @Test
    fun `reports bare local vars in composable lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Row(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            fun Example() {
                Row {
                    var count = 0
                    println(count)
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(12, 17))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
    }

    @Test
    fun `reports bare local vars in top-level composable lambda properties`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val Content: @Composable () -> Unit = {
                var count = 0
                println(count)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(5, 13))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
    }

    @Test
    fun `reports bare local vars in eager lambdas inside composable functions`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example() {
                run {
                    var count = 0
                    println(count)
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(7, 17))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
    }

    @Test
    fun `reports bare local vars in composable getters`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            val value: Int
                @Composable
                get() {
                    var count = 0
                    return count
                }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(7, 17))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
    }

    @Test
    fun `does not report local vars in non-composable lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Button(onClick: () -> Unit, content: @Composable () -> Unit) {
                content()
            }

            @Composable
            fun Example() {
                Button(
                    onClick = {
                        var count = 0
                        println(count)
                    },
                ) {
                    println("content")
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report local vars in non-composable getters`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example() {
                val value: Int
                    get() {
                        var count = 0
                        return count
                    }
                println(value)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report member vars in anonymous objects`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example() {
                val holder = object {
                    var count = 0
                }
                println(holder.count)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report local vars in non-composable inline composable lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            inline fun <T> inlineRemember(calculation: () -> T): T = calculation()

            @Composable
            fun Example() {
                val values = inlineRemember {
                    var index = 0
                    List(3) { index++ }
                }
                println(values)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports local vars in composable lambdas nested in skipped lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            inline fun <T> inlineRemember(calculation: () -> T): T = calculation()

            @Composable
            fun Example() {
                val content = inlineRemember {
                    @Composable {
                        var count = 0
                        println(count)
                    }
                }
                content()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(11, 21))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
    }

    @Test
    fun `reports non-compose delegated local vars`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            import kotlin.reflect.KProperty

            class NonComposeDelegate {
                operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0
                operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = Unit
            }

            @Composable
            fun Example() {
                var count by NonComposeDelegate()
                count = 1
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(13, 13))
            .hasMessage(VarsWithoutStateBackingCheck.VarsWithoutStateBacking)
    }

    @Test
    fun `does not report compose state delegated local vars`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(state: MutableState<Int>) {
                var count by state
                count = 1
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report local vars outside composable functions`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            fun Example() {
                var count = 0
                if (count == 0) {
                    var label = "empty"
                    println(label)
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }
}
