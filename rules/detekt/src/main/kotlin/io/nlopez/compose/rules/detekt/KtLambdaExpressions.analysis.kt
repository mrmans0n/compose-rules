// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtValueArgument

internal fun KtLambdaExpression.isEagerScopeFunctionLambda(): Boolean =
    parent.immediateLambdaArgumentCall()?.isEagerScopeFunctionCall() == true

private tailrec fun PsiElement?.immediateLambdaArgumentCall(): KtCallExpression? = when (this) {
    is KtLambdaArgument -> parent as? KtCallExpression
    is KtValueArgument -> parent?.parent as? KtCallExpression
    is KtAnnotatedExpression -> parent.immediateLambdaArgumentCall()
    is KtLabeledExpression -> parent.immediateLambdaArgumentCall()
    is KtParenthesizedExpression -> parent.immediateLambdaArgumentCall()
    else -> null
}

internal fun KtCallExpression.isEagerScopeFunctionCall(): Boolean = isResolvedCallToAnyNamed(EagerScopeFunctions)

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
