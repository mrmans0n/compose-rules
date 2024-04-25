// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.argumentsUsingModifiers
import io.nlopez.rules.core.util.emitsContent
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isInContentEmittersDenylist
import io.nlopez.rules.core.util.mapSecond
import io.nlopez.rules.core.util.modifierParameter
import io.nlopez.rules.core.util.obtainAllModifierNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.parents

class ModifierNotUsedAtRoot : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) = with(config) {
        val modifier = function.modifierParameter ?: return

        // We only care about the main modifier for this rule
        if (modifier.name != "modifier") return
        val code = function.bodyBlockExpression ?: return

        val modifiers = code.obtainAllModifierNames("modifier").toSet()

        val errors = code.findChildrenByClass<KtCallExpression>()
            .filter { it.calleeExpression?.text?.first()?.isUpperCase() == true }
            .mapNotNull { callExpression ->
                val usage = callExpression.argumentsUsingModifiers(modifiers).firstOrNull() ?: return@mapNotNull null
                callExpression to usage
            }
            .filterNot { (callExpression, _) ->
                // If there is a parent that's a non-content emitter or deny-listed, we don't want to continue
                callExpression.parents.filterIsInstance<KtCallExpression>().any { it.isInContentEmittersDenylist }
            }
            .filter { (callExpression, _) ->
                // we'll need to traverse upwards to the composable root and check if there is any parent that
                // emits content: if this is the case, the main modifier should be used there instead.
                callExpression.findFirstAncestorEmittingContent(stopAt = code) { it.emitsContent } != null
            }
            .mapSecond()

        for (valueArgument in errors) {
            emitter.report(valueArgument, ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace)
        }
    }

    private fun KtCallExpression.findFirstAncestorEmittingContent(
        stopAt: PsiElement,
        isContentEmitterPredicate: (KtCallExpression) -> Boolean,
    ): KtCallExpression? {
        val origin = this
        var current: PsiElement = this
        var result: KtCallExpression? = null
        while (current != stopAt) {
            if (current != origin && current is KtCallExpression && isContentEmitterPredicate(current)) {
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
