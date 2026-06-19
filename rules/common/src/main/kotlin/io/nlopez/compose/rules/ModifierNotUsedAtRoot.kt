// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.argumentsUsingModifiers
import io.nlopez.compose.core.util.emitsContent
import io.nlopez.compose.core.util.findAllChildrenByClass
import io.nlopez.compose.core.util.isFullyShadowed
import io.nlopez.compose.core.util.isInContentEmittersDenylist
import io.nlopez.compose.core.util.mapSecond
import io.nlopez.compose.core.util.modifierParameter
import io.nlopez.compose.core.util.modifierTypeNames
import io.nlopez.compose.core.util.obtainAllModifierNames
import io.nlopez.compose.core.util.rootExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

class ModifierNotUsedAtRoot : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        val modifier = function.modifierParameter(config) ?: return

        // We only care about the main modifier for this rule
        if (modifier.name != "modifier") return
        val code = function.bodyBlockExpression ?: return

        val modifiers = code.obtainAllModifierNames("modifier").toSet()
        val typeNames = modifierTypeNames(config)

        val errors = code.findAllChildrenByClass<KtCallExpression>()
            .filter { it.calleeExpression?.text?.first()?.isUpperCase() == true }
            .mapNotNull { callExpression ->
                // When a call has multiple modifier-using arguments and the first belongs to a
                // shadowed lambda parameter, firstOrNull() would report at the wrong location.
                // Prefer the first argument whose modifier is not from a shadow scope.
                val args = callExpression.argumentsUsingModifiers(modifiers, typeNames)
                if (args.isEmpty()) return@mapNotNull null
                val shadowedNames = callExpression.shadowedModifierNamesUpTo(function, modifiers)
                val usage = args.firstOrNull { arg ->
                    when (val expr = arg.getArgumentExpression()) {
                        is KtReferenceExpression -> expr.text !in shadowedNames
                        is KtDotQualifiedExpression -> expr.rootExpression.text !in shadowedNames
                        else -> true
                    }
                } ?: args.first()
                callExpression to usage
            }
            .filterNot { (callExpression, _) -> callExpression.isFullyShadowed(modifiers, function, typeNames) }
            .filter { (callExpression, _) ->
                // we'll need to traverse upwards to the composable root and check if there is any parent that
                // emits content: if this is the case, the main modifier should be used there instead.
                callExpression.parents.takeWhile { it != code }
                    .filterIsInstance<KtCallExpression>()
                    // If there is a parent that's a non-content emitter or deny-listed, we don't want to continue
                    .takeWhile { !it.isInContentEmittersDenylist(config) }
                    .any { it.emitsContent(config) }
            }
            .mapSecond()

        for (valueArgument in errors) {
            emitter.report(valueArgument, ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace)
        }
    }

    private fun KtCallExpression.shadowedModifierNamesUpTo(stopAt: KtFunction, modifiers: Set<String>): Set<String> =
        parents.takeWhile { it != stopAt }
            .filterIsInstance<KtFunctionLiteral>()
            .flatMap { literal ->
                literal.valueParameters.flatMap { param ->
                    when {
                        param.name != null -> listOfNotNull(param.name.takeIf { it in modifiers })

                        param.destructuringDeclaration != null ->
                            param.destructuringDeclaration!!.entries
                                .mapNotNull { it.name?.takeIf { it in modifiers } }

                        else -> emptyList()
                    }
                }
            }
            .toSet()

    companion object {
        val ComposableModifierShouldBeUsedAtTheTopMostPossiblePlace = """
            The main Modifier of a @Composable should be applied once as a first modifier in the chain to the root-most layout in the component implementation.
            You should move the modifier usage to the appropriate parent Composable.
            See https://mrmans0n.github.io/compose-rules/rules/#modifiers-should-be-used-at-the-top-most-layout-of-the-component for more information.
        """.trimIndent()
    }
}
