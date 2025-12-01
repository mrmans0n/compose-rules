// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import dev.detekt.test.lint
import io.nlopez.compose.rules.ComposableAnnotationNaming
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposableAnnotationNamingCheckTest {

    private val rule = ComposableAnnotationNamingCheck(Config.empty)

    @Test
    fun `passes for non-composable annotations`() {
        @Language("kotlin")
        val code =
            """
            annotation class Banana
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
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
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(2, 18),
            SourceLocation(4, 18),
        )
        for (error in errors) {
            assertThat(error)
                .hasMessage(ComposableAnnotationNaming.ComposableAnnotationDoesNotEndWithComposable)
        }
    }
}
