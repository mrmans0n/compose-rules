// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithInitializer
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

private val DefaultModifierTypeNames = setOf("Modifier", "GlanceModifier")

fun KtCallExpression.parametersBeingUsedFrom(
    parameterNames: Set<String>,
    modifierTypeNames: Set<String> = DefaultModifierTypeNames,
): Set<String> = valueArguments.flatMap { argument ->
    when (val expression = argument.getArgumentExpression()) {
        // if it's MyComposable(modifier) or similar
        is KtReferenceExpression -> listOfNotNull(expression.text.takeIf { it in parameterNames })

        // if it's MyComposable(modifier.fillMaxWidth()) or similar,
        // also handles MyComposable(Modifier.then(modifier)) and chained variants
        is KtDotQualifiedExpression -> expression.parameterNamesUsedIn(parameterNames, modifierTypeNames)

        else -> emptyList()
    }
}.toSet()

private fun KtDotQualifiedExpression.parameterNamesUsedIn(
    parameterNames: Set<String>,
    modifierTypeNames: Set<String>,
): Set<String> = buildSet {
    val rootText = rootExpression.text
    if (rootText in parameterNames) add(rootText)
    // Scan .then() arguments when the chain root is a modifier parameter/alias, a known
    // Modifier type literal (including custom types), or a lowercase local variable (which may
    // itself be a Modifier value). This mirrors the expanded check in argumentsUsingModifiers.
    val shouldScanThenArgs = rootText in parameterNames ||
        rootText in modifierTypeNames ||
        rootText.first().isLowerCase()
    if (shouldScanThenArgs) {
        var current: KtDotQualifiedExpression? = this@parameterNamesUsedIn
        while (current != null) {
            val selector = current.selectorExpression as? KtCallExpression
            if (selector?.calleeExpression?.text == "then") {
                for (arg in selector.valueArguments) {
                    when (val expr = arg.getArgumentExpression()) {
                        is KtReferenceExpression -> if (expr.text in parameterNames) add(expr.text)

                        is KtDotQualifiedExpression -> if (expr.rootExpression.text in parameterNames) {
                            add(expr.rootExpression.text)
                        }

                        else -> {}
                    }
                }
            }
            current = current.receiverExpression as? KtDotQualifiedExpression
        }
    }
}

private fun KtCallExpression.ancestorsParameterNamesSequence(stopAt: PsiElement) = parents.takeWhile { it != stopAt }
    .filterIsInstance<KtCallableDeclaration>()
    .flatMap { it.valueParameters }
    .flatMap { parameter ->
        when {
            // Normal parameters
            parameter.name != null -> listOfNotNull(parameter.name)

            // Destructured parameters
            parameter.destructuringDeclaration != null ->
                parameter.destructuringDeclaration!!
                    .entries
                    .mapNotNull { it.name }

            else -> emptyList()
        }
    }

private fun KtCallExpression.walkbackDeclarationsUntil(stopAt: PsiElement) = walkBackwards(stopAtParent = stopAt)
    .filterIsInstance<KtDeclarationWithInitializer>()
    .flatMap { declaration ->
        when {
            declaration.name != null -> listOfNotNull(declaration.name)
            declaration is KtDestructuringDeclaration -> declaration.entries.mapNotNull { it.name }
            else -> emptyList()
        }.map { it to declaration }
    }

fun KtCallExpression.findShadowingRedeclarations(
    parameterName: String,
    stopAt: PsiElement,
): Sequence<KtDeclarationWithInitializer> = walkbackDeclarationsUntil(stopAt = stopAt)
    .filter { (name, _) -> name == parameterName }
    .mapSecond()

fun KtCallExpression.isFullyShadowed(
    parameterNames: Set<String>,
    origin: PsiElement,
    modifierTypeNames: Set<String> = DefaultModifierTypeNames,
): Boolean {
    val currentNames = parametersBeingUsedFrom(parameterNames, modifierTypeNames)
    if (currentNames.isEmpty()) return false

    // Only skip this call if every modifier it uses comes from a shadow scope.
    // Using any { } here would incorrectly drop calls where one argument is a shadowed lambda-local
    // modifier but another argument is a genuine outer-modifier alias.
    val ancestorNames = ancestorsParameterNamesSequence(stopAt = origin).toSet()
    return currentNames.all { it in ancestorNames }
}
