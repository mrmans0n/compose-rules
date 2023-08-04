// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.emitsContent
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isUsingModifiers
import io.nlopez.rules.core.util.modifierParameter
import io.nlopez.rules.core.util.obtainAllModifierNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction

class ComposeModifierNotUsedAtRoot : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        val modifier = function.modifierParameter ?: return
        if (modifier.name != "modifier") return
        val code = function.bodyBlockExpression ?: return

        val modifiers = code.obtainAllModifierNames("modifier")

        val callExpressions = code.findChildrenByClass<KtCallExpression>()
            .filter { it.calleeExpression?.text?.first()?.isUpperCase() == true }
            .filter { it.isUsingModifiers(modifiers) }
            .filter { callExpression ->
                // we'll need to traverse upwards to the composable root and check if there is any parent that
                // emits content: if this is the case, the main modifier should be used there instead.
                callExpression.findFirstAncestorEmittingContent(stopAt = code) != null
            }

        for (callExpression in callExpressions) {
            emitter.report(callExpression, ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace)
        }
    }

    private fun KtCallExpression.findFirstAncestorEmittingContent(stopAt: PsiElement): KtCallExpression? {
        var current: PsiElement = this
        var result: KtCallExpression? = null
        while (current != stopAt) {
            if (current is KtCallExpression && current.emitsContent) {
                result = current
            }
            current = current.parent
        }
        return result
    }

    companion object {
        val ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace = """
            The main Modifier of a @Composable should be applied once as a first modifier in the chain to the root-most layout in the component implementation.

            See https://mrmans0n.github.io/compose-rules/rules/#TODO for more information.
        """.trimIndent()
    }
}
