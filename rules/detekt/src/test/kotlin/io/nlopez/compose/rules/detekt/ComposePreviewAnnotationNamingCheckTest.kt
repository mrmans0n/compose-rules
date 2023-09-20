// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ComposePreviewAnnotationNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposePreviewAnnotationNamingCheckTest {

    private val rule = ComposePreviewAnnotationNamingCheck(Config.empty)

    @Test
    fun `passes for non-preview annotations`() {
        @Language("kotlin")
        val code =
            """
            annotation class Banana
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `passes for preview annotations with the proper names`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            annotation class PreviewBanana
            @PreviewBanana
            annotation class PreviewDoubleBanana
            @Preview
            @Preview
            annotation class PreviewApple
            @Preview
            @PreviewApple
            annotation class PreviewCombinedApple
            @PreviewBanana
            @PreviewApple
            annotation class PreviewFruitBasket
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors when a multipreview annotation is not correctly named for 1 preview`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            annotation class Banana
            @Preview
            annotation class BananaPreviews
            @BananaPreview
            annotation class WithBananaPreviews
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(2, 18),
            SourceLocation(4, 18),
            SourceLocation(6, 18),
        )
        for (error in errors) {
            assertThat(error)
                .hasMessage(ComposePreviewAnnotationNaming.PreviewAnnotationDoesNotStartWithPreview)
        }
    }

    @Test
    fun `errors when a multipreview annotation is not correctly named for multi previews`() {
        @Language("kotlin")
        val code =
            """
            @Preview
            @Preview
            annotation class BananaPreview
            @BananaPreview
            @BananaPreview
            annotation class BananaPreview
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(3, 18),
            SourceLocation(6, 18),
        )
        for (error in errors) {
            assertThat(error)
                .hasMessage(ComposePreviewAnnotationNaming.PreviewAnnotationDoesNotStartWithPreview)
        }
    }
}
