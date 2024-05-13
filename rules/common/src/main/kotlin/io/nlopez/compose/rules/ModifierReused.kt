// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.emitsContent
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.isAnyShadowed
import io.nlopez.compose.core.util.isUsingModifiers
import io.nlopez.compose.core.util.modifierParameters
import io.nlopez.compose.core.util.obtainAllModifierNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.siblings

class ModifierReused : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) {
        with(config) { if (!function.emitsContent) return }

        val composableBlockExpression = function.bodyBlockExpression ?: return
        val initialModifierNames = with(config) { function.modifierParameters.mapNotNull { it.name }.toSet() }
        if (initialModifierNames.isEmpty()) return

        initialModifierNames
            .map {
                // Try to get all possible names for each modifier by iterating on possible name reassignments until it's stable
                composableBlockExpression.obtainAllModifierNames(it).toSet()
            }
            .forEach { modifierNames ->
                // Find all composable-looking CALL_EXPRESSIONs that are using any of these modifier names
                composableBlockExpression.findChildrenByClass<KtCallExpression>()
                    .filter { it.calleeExpression?.text?.first()?.isUpperCase() == true }
                    .filter { it.isUsingModifiers(modifierNames) }
                    // For those modifiers, we look at the parents and see if any of them is a function that has a param with
                    //  the same name.
                    .filterNot { it.isAnyShadowed(modifierNames, function) }
                    .map { callExpression ->
                        // To get an accurate count (and respecting if/when/whatever different branches)
                        // we'll need to traverse upwards to [function] from each one of these usages
                        // to see the real amount of usages.
                        buildSet<KtCallExpression> {
                            var current: PsiElement = callExpression
                            while (current != composableBlockExpression) {
                                // If the current element is a CALL_EXPRESSION and using modifiers, log it
                                if (current is KtCallExpression && current.isUsingModifiers(modifierNames)) {
                                    add(current)
                                }
                                // If any of the siblings also use any of these, we also log them.
                                // This is for the special case where only sibling composables reuse modifiers
                                addAll(
                                    current.siblings()
                                        .filterIsInstance<KtCallExpression>()
                                        .filter { it.isUsingModifiers(modifierNames) },
                                )
                                current = current.parent
                            }
                        }
                    }
                    // Any set with more than 1 item is interesting to us: means there is a rule violation
                    .filter { it.size > 1 }
                    // At this point we have all the grouping of violations, so we just need to extract all individual
                    // items from them as we are no longer interested in the groupings, but their individual elements
                    .flatten()
                    // We don't want to double report
                    .distinct()
                    .forEach { callExpression ->
                        emitter.report(callExpression, ModifierShouldBeUsedOnceOnly, false)
                    }
            }
    }

    companion object {
        val ModifierShouldBeUsedOnceOnly = """
            Modifiers should only be used once and by the root level layout of a Composable. This is true even if appended to or with other modifiers e.g. 'modifier.fillMaxWidth()'.

            Use Modifier (with a capital 'M') to construct a new Modifier that you can pass to other composables.

            See https://mrmans0n.github.io/compose-rules/rules/#dont-re-use-modifiers for more information.
        """.trimIndent()
    }
}
