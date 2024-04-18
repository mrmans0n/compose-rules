// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.Naming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class NamingCheckTest {

    private val namingRuleAssertThat = assertThatRule { NamingCheck() }

    @Test
    fun `passes when a composable that returns values is lowercase`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun myComposable(): Something { }
            """.trimIndent()
        namingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when a composable that returns values is uppercase but allowed`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun ProfilePresenter(): Something { }
            """.trimIndent()
        namingRuleAssertThat(code)
            .withEditorConfigOverride(
                allowedComposeNamingNames to ".*Presenter",
            )
            .hasNoLintViolations()
    }

    @Test
    fun `passes when a composable that returns nothing or Unit is uppercase`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() { }
                @Composable
                fun MyComposable(): Unit { }
            """.trimIndent()
        namingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when a composable doesn't have a body block, is a property or a lambda`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable() = Text("bleh")

                val composable: Something
                    @Composable get() { }

                val composable: Something
                    @Composable get() = OtherComposable()

                val whatever = @Composable { }
            """.trimIndent()
        namingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when a composable returns a value and is capitalized`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun MyComposable(): Something { }
            """.trimIndent()
        namingRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 5,
                detail = Naming.ComposablesThatReturnResultsShouldBeLowercase,
            ),
        )
    }

    @Test
    fun `passes when a composable returns a value and is capitalized but it's suppressed`() {
        @Language("kotlin")
        val code =
            """
                @Suppress("ComposableNaming")
                @Composable
                fun MyComposable(): Something { }
            """.trimIndent()
        namingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when a composable returns nothing or Unit and is lowercase`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun myComposable() { }

                @Composable
                fun myComposable(): Unit { }
            """.trimIndent()
        namingRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 5,
                detail = Naming.ComposablesThatDoNotReturnResultsShouldBeCapitalized,
            ),
            LintViolation(
                line = 5,
                col = 5,
                detail = Naming.ComposablesThatDoNotReturnResultsShouldBeCapitalized,
            ),
        )
    }

    @Test
    fun `passes when a composable returns nothing or Unit and is lowercase but it's suppressed`() {
        @Language("kotlin")
        val code =
            """
                @Suppress("ComposableNaming")
                @Composable
                fun myComposable() { }

                @Suppress("ComposableNaming")
                @Composable
                fun myComposable(): Unit { }
            """.trimIndent()
        namingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when a composable returns nothing or Unit and is lowercase but has a receiver`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun Potato.myComposable() { }

                @Composable
                fun Banana.myComposable(): Unit { }
            """.trimIndent()

        namingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes when a composable is an operator function even if the naming should be wrong`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                operator fun invoke() { }
            """.trimIndent()

        namingRuleAssertThat(code).hasNoLintViolations()
    }
}
