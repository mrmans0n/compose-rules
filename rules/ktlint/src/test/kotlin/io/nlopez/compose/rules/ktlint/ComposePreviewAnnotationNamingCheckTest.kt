// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ComposePreviewAnnotationNaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ComposePreviewAnnotationNamingCheckTest {

    private val ruleAssertThat = assertThatRule { ComposePreviewAnnotationNamingCheck() }

    @Test
    fun `passes for non-preview annotations`() {
        @Language("kotlin")
        val code =
            """
            annotation class Banana
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
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
        ruleAssertThat(code).hasNoLintViolations()
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
        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 18,
                detail = ComposePreviewAnnotationNaming.PreviewAnnotationDoesNotStartWithPreview,
            ),
            LintViolation(
                line = 4,
                col = 18,
                detail = ComposePreviewAnnotationNaming.PreviewAnnotationDoesNotStartWithPreview,
            ),
            LintViolation(
                line = 6,
                col = 18,
                detail = ComposePreviewAnnotationNaming.PreviewAnnotationDoesNotStartWithPreview,
            ),
        )
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
        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 18,
                detail = ComposePreviewAnnotationNaming.PreviewAnnotationDoesNotStartWithPreview,
            ),
            LintViolation(
                line = 6,
                col = 18,
                detail = ComposePreviewAnnotationNaming.PreviewAnnotationDoesNotStartWithPreview,
            ),
        )
    }
}
