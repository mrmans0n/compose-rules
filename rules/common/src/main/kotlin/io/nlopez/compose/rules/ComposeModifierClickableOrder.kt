// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.argumentsUsingModifiers
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.obtainAllModifierNames
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument

class ComposeModifierClickableOrder : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        val code = function.bodyBlockExpression ?: return

        val modifiers = code.obtainAllModifierNames("modifier")

        val suspiciousOrderModifiers = code.findChildrenByClass<KtCallExpression>()
            .filter { it.calleeExpression?.text?.first()?.isUpperCase() == true }
            .flatMap { callExpression ->
                callExpression.argumentsUsingModifiers(modifiers + "Modifier")
            }
            // We only want chains of more than 1 modifier
            .mapNotNull { argument -> argument.getArgumentExpression() }
            .filterIsInstance<KtDotQualifiedExpression>()
            .mapNotNull { chain -> chain.findCallExpressionSuspiciousOrder() }

        for (methodInvocation in suspiciousOrderModifiers) {
            emitter.report(methodInvocation, ModifierChainWithSuspiciousOrder)
        }
    }

    private fun KtDotQualifiedExpression.findCallExpressionSuspiciousOrder(): KtCallExpression? {
        // KtDotQualifiedExpression are resolved from end to beginning, so:
        // Modifier.a().b().c() -> (2) + CallExpression c ==> 3
        // Modifier.a().b() -> (1) + CallExpression b ==> 2
        // Modifier.a() -> (root expression) + CallExpression a ==> 1

        var currentReceiver: KtExpression = receiverExpression
        var currentSelector: KtExpression? = selectorExpression

        var shapeAlteringCandidate = false
        while (currentSelector != null) {
            if (currentSelector is KtCallExpression) {
                when {
                    shapeAlteringCandidate && currentSelector.isClickableInteraction -> return currentSelector
                    currentSelector.isClipWithShape -> shapeAlteringCandidate = true
                    currentSelector.isBorderWithShape -> shapeAlteringCandidate = true
                    currentSelector.isBackgroundWithShape -> shapeAlteringCandidate = true
                    currentSelector.isThen -> {
                        val param = currentSelector.valueArguments.firstOrNull()
                        if (param != null) {
                            val argumentExpression = param.getArgumentExpression()
                            if (argumentExpression is KtIfExpression) {
                                // If any of the two branches from the `if` passes the same checks for sus methods,
                                // we flag them as well.
                                val suspicious = sequenceOf(argumentExpression.then, argumentExpression.`else`)
                                    .filterNotNull()
                                    .filterIsInstance<KtCallExpression>()
                                    .any { it.isClipWithShape || it.isBackgroundWithShape || it.isBorderWithShape }

                                if (suspicious) {
                                    shapeAlteringCandidate = true
                                }
                            }
                        }
                    }

                    else -> {
                        // no-op
                    }
                }
            }

            if (currentReceiver is KtDotQualifiedExpression) {
                currentSelector = currentReceiver.selectorExpression
                currentReceiver = currentReceiver.receiverExpression
            } else {
                // If currentReceiver isn't a dot qualified expression anymore it means that we reached the top of the
                // chain, and we are not interesting on it anymore.
                currentSelector = null
            }
        }

        return null
    }

    private val KtCallExpression.isClickableInteraction: Boolean
        get() = calleeExpression?.text in interactionModifiers

    private val KtCallExpression.isThen: Boolean
        get() = calleeExpression?.text == "then"
    private val KtCallExpression.isClipWithShape: Boolean
        get() = calleeExpression?.text == "clip" && valueArguments.any { it.isNamedShape || it.referencesShape }

    private val KtCallExpression.isBackgroundWithShape: Boolean
        get() = calleeExpression?.text == "background" && valueArguments.any { it.isNamedShape || it.referencesShape }

    private val KtCallExpression.isBorderWithShape: Boolean
        get() = calleeExpression?.text == "border" && valueArguments.any { it.isNamedShape || it.referencesShape }

    private val KtValueArgument.isNamedShape: Boolean
        get() = isNamed() && name == "shape"

    private val KtValueArgument.referencesShape: Boolean
        get() = when (val expression = getArgumentExpression()) {
            // MyShape()
            is KtCallExpression -> expression.calleeExpression?.text?.endsWith("Shape") == true
            // MyShape
            is KtReferenceExpression -> expression.text.endsWith("Shape")
            // if (x) MyShape else MyOtherShape
            is KtIfExpression -> expression.then?.text?.endsWith("Shape") == true ||
                expression.`else`?.text?.endsWith("Shape") == true
            else -> false
        }

    companion object {
        private val interactionModifiers = setOf(
            "clickable",
            "selectable",
            "toggleable",
            "triStateToggleable",
            "combinedClickable",
        )

        // TODO write the actual error
        val ModifierChainWithSuspiciousOrder = """
            This order of modifiers is likely to cause visual issues. You should have your clickable modifiers after modifiers that use shapes, so that the clickable selected area takes into account the change in shape as well.

            See https://mrmans0n.github.io/compose-rules/rules/#TODO for more information.
        """.trimIndent()
    }
}
