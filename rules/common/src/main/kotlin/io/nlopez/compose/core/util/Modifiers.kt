// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core.util

import io.nlopez.compose.core.ComposeKtConfig
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentName

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
        // Find usages in child composable blocks
        tempModifierNames += findAllChildrenByClass<KtBlockExpression>()
            .flatMap { block -> block.findModifierManipulations { tempModifierNames.contains(it) } }
    }
    return tempModifierNames.toList()
}

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

fun KtCallExpression.isUsingModifiers(modifierNames: Set<String>): Boolean =
    argumentsUsingModifiers(modifierNames).isNotEmpty()

fun KtCallExpression.argumentsUsingModifiers(modifierNames: Set<String>): List<KtValueArgument> =
    valueArguments.filter { argument ->
        when (val expression = argument.getArgumentExpression()) {
            // if it's MyComposable(modifier) or similar
            is KtReferenceExpression -> {
                expression.text in modifierNames
            }

            // if it's MyComposable(modifier.fillMaxWidth()) or similar
            is KtDotQualifiedExpression -> {
                // On cases of multiple nested KtDotQualifiedExpressions (e.g. multiple chained methods)
                // we need to iterate until we find the start of the chain
                expression.rootExpression.text in modifierNames
            }

            else -> false
        }
    }

private val ModifierNames by lazy {
    setOf(
        "Modifier",
        "GlanceModifier",
    )
}

fun KtCallableDeclaration.isModifier(config: ComposeKtConfig): Boolean =
    typeReference?.text in ModifierNames + config.getSet("customModifiers", emptySet())

fun KtCallableDeclaration.isModifierReceiver(config: ComposeKtConfig): Boolean =
    receiverTypeReference?.text in ModifierNames + config.getSet("customModifiers", emptySet())

fun KtFunction.modifierParameter(config: ComposeKtConfig): KtParameter? {
    val modifiers = valueParameters.filter { it.isModifier(config) }
    return modifiers.firstOrNull { it.name == "modifier" } ?: modifiers.firstOrNull()
}

fun KtFunction.modifierParameters(config: ComposeKtConfig): List<KtParameter> =
    valueParameters.filter { it.isModifier(config) }
