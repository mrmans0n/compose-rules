// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal fun KtLambdaExpression.isEagerScopeFunctionLambda(): Boolean {
    val callExpression = generateSequence(parent) { element -> element.parent }
        .filterIsInstance<KtCallExpression>()
        .firstOrNull()

    return callExpression?.isResolvedCallToAnyNamed(EagerScopeFunctions) == true
}

private val EagerScopeFunctions = setOf(
    "kotlin.run",
    "kotlin.with",
    "kotlin.let",
    "kotlin.also",
    "kotlin.apply",
    "kotlin.takeIf",
    "kotlin.takeUnless",
    "kotlin.repeat",
    "kotlin.collections.forEach",
    "kotlin.sequences.forEach",
)
