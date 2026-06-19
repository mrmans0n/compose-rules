// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core.util

import io.nlopez.compose.core.ComposeKtConfig
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentName
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 *  Try to get all possible names by iterating on possible name reassignments until it's stable
 */
fun KtBlockExpression.obtainAllModifierNames(initialName: String): List<String> {
    var lastSize = 0
    val tempModifierNames = mutableSetOf(initialName)
    while (lastSize < tempModifierNames.size) {
        lastSize = tempModifierNames.size
        // Find usages in the current block (the original composable)
        tempModifierNames += findModifierManipulations { tempModifierNames.contains(it) }
        // Find usages in child blocks, but skip any block inside a lambda that shadows a
        // modifier name — aliases created there belong to the lambda's local modifier, not the
        // outer composable's, so including them would cause false positives.
        tempModifierNames += findAllChildrenByClass<KtBlockExpression>()
            .filter { block -> !block.isInsideShadowingLambda(tempModifierNames) }
            .flatMap { block -> block.findModifierManipulations { tempModifierNames.contains(it) } }
    }
    return tempModifierNames.toList()
}

private fun KtBlockExpression.isInsideShadowingLambda(modifierNames: Set<String>): Boolean =
    parents.filterIsInstance<KtFunctionLiteral>()
        .any { literal -> literal.valueParameters.any { param -> param.name in modifierNames } }

/**
 * Find references to modifier as a property in case they try to modify or reuse the modifier that way
 * E.g. val modifier2 = if (X) modifier.blah() else modifier.bleh()
 */
private fun KtBlockExpression.findModifierManipulations(contains: (String) -> Boolean): List<String> = statements
    .filterIsInstance<KtProperty>()
    .flatMap { property ->
        property.findAllChildrenByClass<KtReferenceExpression>()
            .filter { referenceExpression ->
                val parent = referenceExpression.parent
                parent !is KtCallExpression &&
                    parent !is KtValueArgumentName &&
                    contains(referenceExpression.text)
            }
            .map { property }
    }
    .mapNotNull { it.nameIdentifier?.text }

fun KtCallExpression.isUsingModifiers(
    modifierNames: Set<String>,
    modifierTypeNames: Set<String> = ModifierNames,
): Boolean = argumentsUsingModifiers(modifierNames, modifierTypeNames).isNotEmpty()

fun KtCallExpression.argumentsUsingModifiers(
    modifierNames: Set<String>,
    modifierTypeNames: Set<String> = ModifierNames,
): List<KtValueArgument> = valueArguments.filter { argument ->
    when (val expression = argument.getArgumentExpression()) {
        // if it's MyComposable(modifier) or similar
        is KtReferenceExpression -> {
            expression.text in modifierNames
        }

        // if it's MyComposable(modifier.fillMaxWidth()) or similar,
        // also handles MyComposable(Modifier.then(modifier)) and chained variants
        is KtDotQualifiedExpression -> {
            // On cases of multiple nested KtDotQualifiedExpressions (e.g. multiple chained methods)
            // we need to iterate until we find the start of the chain
            val rootText = expression.rootExpression.text
            rootText in modifierNames ||
                // Only scan .then() args when the chain root is a known Modifier type literal
                // (Modifier, GlanceModifier, or a configured custom modifier type). Guarding
                // against exact type names prevents false positives from unrelated chains like
                // SomePipeline.then(modifier) where the receiver is not a Modifier at all.
                (rootText in modifierTypeNames && expression.hasModifierAsChainArgument(modifierNames))
        }

        else -> false
    }
}

// Checks if a modifier name appears as a direct argument to a .then() call anywhere in a
// dot-qualified chain. Restricting to .then() avoids false positives from unrelated chains like
// PainterFactory.create(modifier) and automatically covers custom modifier types.
private fun KtDotQualifiedExpression.hasModifierAsChainArgument(modifierNames: Set<String>): Boolean {
    var current: KtDotQualifiedExpression? = this
    while (current != null) {
        val selector = current.selectorExpression as? KtCallExpression
        if (selector?.calleeExpression?.text == "then") {
            for (arg in selector.valueArguments) {
                when (val expr = arg.getArgumentExpression()) {
                    is KtReferenceExpression -> if (expr.text in modifierNames) return true
                    is KtDotQualifiedExpression -> if (expr.rootExpression.text in modifierNames) return true
                    else -> {}
                }
            }
        }
        current = current.receiverExpression as? KtDotQualifiedExpression
    }
    return false
}

private val ModifierNames by lazy {
    setOf(
        "Modifier",
        "GlanceModifier",
    )
}

fun modifierTypeNames(config: ComposeKtConfig): Set<String> =
    ModifierNames + config.getSet("customModifiers", emptySet())

fun KtCallableDeclaration.isModifier(config: ComposeKtConfig): Boolean =
    typeReference?.text in modifierTypeNames(config)

fun KtCallableDeclaration.isModifierReceiver(config: ComposeKtConfig): Boolean =
    receiverTypeReference?.text in modifierTypeNames(config)

fun KtFunction.modifierParameter(config: ComposeKtConfig): KtParameter? {
    val modifiers = valueParameters.filter { it.isModifier(config) }
    return modifiers.firstOrNull { it.name == "modifier" } ?: modifiers.firstOrNull()
}

fun KtFunction.modifierParameters(config: ComposeKtConfig): List<KtParameter> =
    valueParameters.filter { it.isModifier(config) }
