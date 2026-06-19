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

fun KtCallExpression.parametersBeingUsedFrom(parameterNames: Set<String>): Set<String> =
    valueArguments.flatMap { argument ->
        when (val expression = argument.getArgumentExpression()) {
            // if it's MyComposable(modifier) or similar
            is KtReferenceExpression -> listOfNotNull(expression.text.takeIf { it in parameterNames })

            // if it's MyComposable(modifier.fillMaxWidth()) or similar,
            // also handles MyComposable(Modifier.then(modifier)) and chained variants
            is KtDotQualifiedExpression -> expression.parameterNamesUsedIn(parameterNames)

            else -> emptyList()
        }
    }.toSet()

private fun KtDotQualifiedExpression.parameterNamesUsedIn(parameterNames: Set<String>): Set<String> = buildSet {
    if (rootExpression.text in parameterNames) add(rootExpression.text)
    var current: KtDotQualifiedExpression? = this@parameterNamesUsedIn
    while (current != null) {
        val selector = current.selectorExpression as? KtCallExpression
        if (selector != null) {
            for (arg in selector.valueArguments) {
                when (val expr = arg.getArgumentExpression()) {
                    is KtReferenceExpression -> if (expr.text in parameterNames) add(expr.text)

                    is KtDotQualifiedExpression -> if (expr.rootExpression.text in
                        parameterNames
                    ) {
                        add(expr.rootExpression.text)
                    }

                    else -> {}
                }
            }
        }
        current = current.receiverExpression as? KtDotQualifiedExpression
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

fun KtCallExpression.isAnyShadowed(parameterNames: Set<String>, origin: PsiElement): Boolean {
    val currentNames = parametersBeingUsedFrom(parameterNames)

    // For those modifiers, we look at the parents and see if any of them is a function that has a param with
    //  the same name.
    return ancestorsParameterNamesSequence(stopAt = origin).any { it in currentNames }
}
