// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ComposableAnnotationNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposableAnnotationNamingCheckTest {

    private val ruleAssertThat = assertThatRule { ComposableAnnotationNamingCheck() }

    @Test
    fun `passes for non-composable annotations`() {
        @Language("kotlin")
        val code =
            """
            annotation class Banana
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `passes for composable annotations with the proper names`() {
        @Language("kotlin")
        val code =
            """
            @ComposableTargetMarker
            annotation class BananaComposable
            @ComposableTargetMarker
            annotation class AppleComposable
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `errors when a composable annotation is not correctly named`() {
        @Language("kotlin")
        val code =
            """
            @ComposableTargetMarker
            annotation class Banana
            @ComposableTargetMarker
            annotation class Apple
            """.trimIndent()
        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 18,
                detail = ComposableAnnotationNaming.ComposableAnnotationDoesNotEndWithComposable,
            ),
            LintViolation(
                line = 4,
                col = 18,
                detail = ComposableAnnotationNaming.ComposableAnnotationDoesNotEndWithComposable,
            ),
        )
    }
}
