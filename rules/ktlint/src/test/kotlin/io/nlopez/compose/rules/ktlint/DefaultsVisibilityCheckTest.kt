// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.DefaultsVisibility.Companion.createMessage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class DefaultsVisibilityCheckTest {

    private val modifierRuleAssertThat = assertThatRule { DefaultsVisibilityCheck() }

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
        modifierRuleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 1,
                col = 17,
                detail = createMessage("public", "MyComposableDefaults", "internal"),
            ),
            LintViolation(
                line = 4,
                col = 16,
                detail = createMessage("internal", "MyOtherComposableDefaults", "private"),
            ),
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
        modifierRuleAssertThat(code).hasNoLintViolations()
    }
}
