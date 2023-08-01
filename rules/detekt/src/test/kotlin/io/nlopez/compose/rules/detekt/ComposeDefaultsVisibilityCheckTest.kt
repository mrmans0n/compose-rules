// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ComposeDefaultsVisibility.Companion.createMessage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposeDefaultsVisibilityCheckTest {

    private val rule = ComposeDefaultsVisibilityCheck(Config.empty)

    @Test
    fun `errors when a defaults object has less visibility than the composable that uses it`() {
        @Language("kotlin")
        val code =
            """
                internal object MyComposableDefaults
                @Composable
                fun MyComposable(someParam: Bleh = MyComposableDefaults.someParam) { }
                private object MyOtherComposableDefaults
                @Composable
                internal fun MyOtherComposable() {
                    val someUsage = MyOtherComposableDefaults.someParam.someMethod()
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(1, 17),
                SourceLocation(4, 16),
            )
        assertThat(errors[0]).hasMessage(
            createMessage("public", "MyComposableDefaults", "internal"),
        )
        assertThat(errors[1]).hasMessage(
            createMessage("internal", "MyOtherComposableDefaults", "private"),
        )
    }

    @Test
    fun `passes when a defaults object has the same visibility as any of the overloaded composables that match it`() {
        @Language("kotlin")
        val code =
            """
                object MyComposableDefaults
                @Composable
                fun MyComposable(someParam: Bleh = MyComposableDefaults.someParam) { }
                internal object MyOtherComposableDefaults
                @Composable
                internal fun MyOtherComposable() {
                    val someUsage = MyOtherComposableDefaults.someParam.someMethod()
                }
                object MyThirdComposableDefaults
                @Composable
                fun MyThirdComposable(a: A) {
                    val someUsage = MyThirdComposableDefaults.someParam
                }
                @Composable
                internal fun MyThirdComposable(b: B) {
                    val someUsage = MyThirdComposableDefaults.someParam
                }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
