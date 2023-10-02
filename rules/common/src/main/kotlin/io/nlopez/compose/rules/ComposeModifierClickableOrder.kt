// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.argumentsUsingModifiers
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.obtainAllModifierNames
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction

class ComposeModifierClickableOrder : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        val code = function.bodyBlockExpression ?: return

        val modifiers = code.obtainAllModifierNames("modifier")

        val composables = code.findChildrenByClass<KtCallExpression>()
            .filter { it.calleeExpression?.text?.first()?.isUpperCase() == true }
            .flatMap { callExpression ->
                callExpression.argumentsUsingModifiers(modifiers + "Modifier")
                    // We only want chains of more than 1 modifier
                    .filter { argument -> argument.getArgumentExpression() is KtDotQualifiedExpression }
                    .map { argument -> callExpression to argument }
            }
            .filter { (callExpression, valueArgument) ->
                // TODO walk around the argument chains
                true
            }

    }

    private fun KtDotQualifiedExpression.analyzeChain() {
        // KtDotQualifiedExpression are resolved from end to beginning, so:
        // Modifier.a().b().c() -> (2) + CallExpression c ==> 3
        // Modifier.a().b() -> (1) + CallExpression b ==> 2
        // Modifier.a() -> (root expression) + CallExpression a ==> 1

        val problematicCallExpressions = mutableListOf<KtCallExpression>()
        var current: KtExpression = receiverExpression
        while (current is KtDotQualifiedExpression) {
            current as KtDotQualifiedExpression

            // TODO is selector alright? double check
            val selector = current.selectorExpression
            if (selector is KtCallExpression) {
                if (selector.name in interactionModifiers) {
                    problematicCallExpressions.add(selector)
                }
            }

            current = current.receiverExpression
        }
    }

    companion object {
        private val interactionModifiers = setOf(
            "clickable",
            "selectable",
            "toggleable",
            "triStateToggleable",
            "combinedClickable"
        )

        val ModifiersAreSupposedToBeCalledModifierWhenAlone = """
            Modifier parameters should be called `modifier`.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-modifiers-properly for more information.
        """.trimIndent()
        val ModifiersAreSupposedToEndInModifierWhenMultiple = """
            Modifier parameters should be called `modifier` or end in `Modifier` if there are more than one in the same @Composable.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-modifiers-properly for more information.
        """.trimIndent()
    }
}
