// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.isPreview
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class ComposePreviewPublic : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        // We only want previews
        if (!function.isPreview) return
        // We only care about public methods
        if (!function.isPublic) return

        emitter.report(function, ComposablesPreviewShouldNotBePublic, true)
        if (autoCorrect) {
            function.addModifier(KtTokens.PRIVATE_KEYWORD)
        }
    }

    companion object {
        val ComposablesPreviewShouldNotBePublic = """
            Composables annotated with @Preview that are used only for previewing the UI should not be public.

            See https://mrmans0n.github.io/compose-rules/rules/#preview-composables-should-not-be-public for more information.
        """.trimIndent()
    }
}
