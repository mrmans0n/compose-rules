// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.emitsContent
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.modifierParameter
import io.nlopez.rules.core.util.obtainAllModifierNames
import io.nlopez.rules.core.util.rootExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument

class ComposeModifierNotUsedAtRoot : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        val modifier = function.modifierParameter ?: return
        if (modifier.name != "modifier") return
        val code = function.bodyBlockExpression ?: return

        val modifiers = code.obtainAllModifierNames("modifier")

        val errors = code.findChildrenByClass<KtCallExpression>()
            .filter { it.calleeExpression?.text?.first()?.isUpperCase() == true }
            .mapNotNull { callExpression ->
                val usage = callExpression.getModifierUsage(modifiers) ?: return@mapNotNull null
                callExpression to usage
            }
            .filter { (callExpression, _) ->
                // we'll need to traverse upwards to the composable root and check if there is any parent that
                // emits content: if this is the case, the main modifier should be used there instead.
                callExpression.findFirstAncestorEmittingContent(stopAt = code) != null
            }
            .map { (_, valueArgument) -> valueArgument }

        for (valueArgument in errors) {
            emitter.report(valueArgument, ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace)
        }
    }

    private fun KtCallExpression.getModifierUsage(modifierNames: List<String>): KtValueArgument? =
        valueArguments.firstOrNull { argument ->
            when (val expression = argument.getArgumentExpression()) {
                // if it's MyComposable(modifier) or similar
                is KtReferenceExpression -> {
                    modifierNames.contains(expression.text)
                }
                // if it's MyComposable(modifier.fillMaxWidth()) or similar
                is KtDotQualifiedExpression -> {
                    // On cases of multiple nested KtDotQualifiedExpressions (e.g. multiple chained methods)
                    // we need to iterate until we find the start of the chain
                    modifierNames.contains(expression.rootExpression.text)
                }

                else -> false
            }
        }

    private fun KtCallExpression.findFirstAncestorEmittingContent(stopAt: PsiElement): KtCallExpression? {
        val origin = this
        var current: PsiElement = this
        var result: KtCallExpression? = null
        while (current != stopAt) {
            if (current != origin && current is KtCallExpression && current.emitsContent) {
                result = current
            }
            current = current.parent
        }
        return result
    }

    companion object {
        val ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace = """
            The main Modifier of a @Composable should be applied once as a first modifier in the chain to the root-most layout in the component implementation.

            You should move the modifier usage to the appropriate parent Composable.

            See https://mrmans0n.github.io/compose-rules/rules/#modifiers-should-be-used-at-the-top-most-layout-of-the-component for more information.
        """.trimIndent()
    }
}
