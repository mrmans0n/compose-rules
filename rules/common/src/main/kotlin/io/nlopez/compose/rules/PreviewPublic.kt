// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import com.intellij.psi.impl.source.tree.LeafPsiElement
import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.ifFix
import io.nlopez.compose.core.util.firstChildLeafOrSelf
import io.nlopez.compose.core.util.isPreview
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class PreviewPublic : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        // We only want previews
        if (!function.isPreview) return
        // We only care about public methods
        if (!function.isPublic) return

        emitter.report(function, ComposablesPreviewShouldNotBePublic, true).ifFix {
            // Ideally if the kotlin embeddable compiler exposes what we need, this would be it:
            //  function.addModifier(KtTokens.PRIVATE_KEYWORD)

            // For now we need to do it by hand with ASTNode: find the "fun" modifier, and prepend "private".
            val node = function.node.findChildByType(org.jetbrains.kotlin.lexer.KtTokens.FUN_KEYWORD)
                ?.firstChildLeafOrSelf() as? LeafPsiElement
                ?: return@ifFix
            node.rawReplaceWithText(
                org.jetbrains.kotlin.lexer.KtTokens.PRIVATE_KEYWORD.value + " " +
                    org.jetbrains.kotlin.lexer.KtTokens.FUN_KEYWORD.value,
            )
        }
    }

    companion object {
        val ComposablesPreviewShouldNotBePublic = """
            Composables annotated with @Preview that are used only for previewing the UI should not be public.
            See https://mrmans0n.github.io/compose-rules/rules/#preview-composables-should-not-be-public for more information.
        """.trimIndent()
    }
}
