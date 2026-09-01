// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.RuleName
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class MissingNonRestartableComposableCheckTest {

    private val rule = MissingNonRestartableComposableCheck(Config.empty)

    @Test
    fun `rule is registered`() {
        val rules = ComposeRuleSetProvider().instance().rules

        assertThat(rules).containsKey(RuleName("MissingNonRestartableComposable"))
    }

    @Test
    fun `reports a composable that only forwards to one composable child`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Content(value: String) {}

            @Composable
            fun Wrapper(value: String) = Content(value)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1).hasTextLocations("Wrapper")
    }

    @Test
    fun `does not report preview composables`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            annotation class Preview

            @Preview
            annotation class Variants

            @Composable
            fun Content() {}

            @Preview
            @Composable
            fun DirectPreview() = Content()

            @Variants
            @Composable
            fun CustomPreview() = Content()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report a wrapper around a value-returning composable`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun ValueContent(): String = "value"

            @Composable
            fun Wrapper() = ValueContent()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports single-call block bodies and parenthesized calls`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Content(value: String) {}

            @Composable
            fun BlockWrapper(value: String) {
                Content(value)
            }

            @Composable
            fun ParenthesizedWrapper(value: String) = (((Content(value))))
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(2).hasTextLocations("BlockWrapper", "ParenthesizedWrapper")
    }

    @Test
    fun `does not report a wrapper that calculates an argument`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Content(value: String) {}

            @Composable
            fun Wrapper(value: String) = Content(value.uppercase())
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports a wrapper that passes compiler-static values`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            object Defaults

            enum class Choice { First }

            @Composable
            fun Content(text: String, count: Int, defaults: Defaults, choice: Choice, host: Host) {}

            class Host {
                @Composable
                fun Wrapper() = Content("fixed", 1 + 2, Defaults, Choice.First, this)
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1).hasTextLocations("Wrapper")
    }

    @Test
    fun `does not report a wrapper with a dynamic default expression`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            fun defaultValue(): String = "value"

            @Composable
            fun Content(value: String) {}

            @Composable
            fun Wrapper(value: String = defaultValue()) = Content(value)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports a wrapper with compiler-static defaults`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            object Defaults

            @Composable
            fun Content(value: String, defaults: Defaults) {}

            @Composable
            fun Wrapper(value: String = "value", defaults: Defaults = Defaults) =
                Content(value, defaults)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1).hasTextLocations("Wrapper")
    }

    @Test
    fun `only reports qualified calls with forwarded receivers`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Host
            class Holder(val host: Host)

            @Composable
            fun Host.Content(value: String) {}

            @Composable
            fun ForwardedReceiver(host: Host, value: String) = host.Content(value)

            @Composable
            fun PropertyReceiver(holder: Holder, value: String) = holder.host.Content(value)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1).hasTextLocations("ForwardedReceiver")
    }

    @Test
    fun `does not report wrappers or children with explicit group contracts`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            annotation class NonRestartableComposable
            annotation class NonSkippableComposable
            annotation class ExplicitGroupsComposable

            @Composable
            fun Content() {}

            @Composable
            @NonRestartableComposable
            fun AlreadyNonRestartable() = Content()

            @Composable
            @NonSkippableComposable
            fun ExplicitlyNonSkippable() = Content()

            @Composable
            @ExplicitGroupsComposable
            fun ExplicitGroups() = Content()

            @Composable
            @ReadOnlyComposable
            fun ReadOnlyContent() {}

            @Composable
            fun ReadOnlyWrapper() = ReadOnlyContent()

            @Composable
            @NonRestartableComposable
            fun NonRestartableChild() = Content()

            @Composable
            fun NonRestartableChildWrapper() = NonRestartableChild()

            @Composable
            @NonSkippableComposable
            fun NonSkippableChild() = Content()

            @Composable
            fun NonSkippableChildWrapper() = NonSkippableChild()

            @Composable
            @ExplicitGroupsComposable
            fun ExplicitGroupsChild() = Content()

            @Composable
            fun ExplicitGroupsChildWrapper() = ExplicitGroupsChild()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report non-concrete wrappers or children`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Content() {}

            @Composable
            inline fun InlineWrapper() = Content()

            open class Base {
                @Composable
                open fun OpenWrapper() = Content()
            }

            class Implementation : Base() {
                @Composable
                override fun OpenWrapper() = Content()
            }

            interface Contract {
                @Composable
                fun InterfaceWrapper() = Content()
            }

            fun Host() {
                @Composable
                fun LocalWrapper() = Content()
            }

            @Composable
            inline fun InlineChild() = Content()

            @Composable
            fun InlineChildWrapper() = InlineChild()

            @Composable
            fun OpenChildWrapper(base: Base) = base.OpenWrapper()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report a Unit wrapper that ignores a composable result`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun ValueContent(): String = "value"

            @Composable
            fun Wrapper() {
                ValueContent()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports a composable property getter that only forwards to one child`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Content() {}

            val Wrapper: Unit
                @Composable
                get() = Content()
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report calls with lambdas spreads or safe calls`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Content {
                @Composable
                fun Render() {}
            }

            @Composable
            fun Child(callback: () -> Unit, vararg values: String) {}

            @Composable
            fun FunctionParameterWrapper(content: @Composable () -> Unit) = content()

            @Composable
            fun LambdaArgumentWrapper() = Child({}, "value")

            @Composable
            fun SpreadWrapper(values: Array<String>) = Child({}, *values)

            @Composable
            fun SafeCallWrapper(content: Content?) {
                content?.Render()
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports a wrapper around a compiled composable`() {
        @Language("kotlin")
        val dependency = """
            package com.example.dependency

            import com.example.compose.fake.Composable

            class CompiledContent {
                @Composable
                fun Render(value: String) {}
            }
        """.trimIndent()

        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            import com.example.dependency.CompiledContent

            @Composable
            fun Wrapper(content: CompiledContent, value: String) = content.Render(value)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code, dependency)

        assertThat(findings).hasSize(1).hasTextLocations("Wrapper")
    }
}
