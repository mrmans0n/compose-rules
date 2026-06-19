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
    parent.immediateLambdaArgumentCall()?.isEagerScopeFunctionCall(this) == true

internal fun KtLambdaExpression.isEagerStdlibScopeFunctionLambda(): Boolean =
    parent.immediateLambdaArgumentCall()?.isEagerStdlibScopeFunctionCall() == true

private tailrec fun PsiElement?.immediateLambdaArgumentCall(): KtCallExpression? = when (this) {
    is KtLambdaArgument -> parent as? KtCallExpression
    is KtValueArgument -> parent?.parent as? KtCallExpression
    is KtAnnotatedExpression -> parent.immediateLambdaArgumentCall()
    is KtLabeledExpression -> parent.immediateLambdaArgumentCall()
    is KtParenthesizedExpression -> parent.immediateLambdaArgumentCall()
    else -> null
}

internal fun KtCallExpression.isEagerScopeFunctionCall(lambdaExpression: KtLambdaExpression): Boolean =
    isResolvedCallToAnyNamed(EagerScopeFunctions) || isResolvedInlineArgument(lambdaExpression)

internal fun KtCallExpression.isEagerScopeFunctionCall(): Boolean = isResolvedCallToAnyNamed(EagerScopeFunctions)

private fun KtCallExpression.isEagerStdlibScopeFunctionCall(): Boolean =
    isResolvedCallToAnyNamed(EagerStdlibScopeFunctions)

private val EagerStdlibScopeFunctions = setOf(
    "kotlin.run",
    "kotlin.with",
    "kotlin.let",
    "kotlin.also",
    "kotlin.apply",
    "kotlin.takeIf",
    "kotlin.takeUnless",
    "kotlin.repeat",
    "kotlin.text.buildString",
    "kotlin.collections.buildList",
    "kotlin.collections.buildSet",
    "kotlin.collections.buildMap",
    "kotlin.collections.forEach",
    "kotlin.collections.forEachIndexed",
    "kotlin.collections.map",
    "kotlin.collections.mapIndexed",
    "kotlin.collections.mapNotNull",
    "kotlin.collections.mapIndexedNotNull",
    "kotlin.collections.associate",
    "kotlin.collections.associateBy",
    "kotlin.collections.associateWith",
    "kotlin.collections.groupBy",
    "kotlin.collections.partition",
    "kotlin.collections.filter",
    "kotlin.collections.filterNot",
    "kotlin.collections.filterNotNull",
    "kotlin.collections.flatMap",
    "kotlin.collections.flatMapIndexed",
    "kotlin.collections.fold",
    "kotlin.collections.foldIndexed",
    "kotlin.collections.reduce",
    "kotlin.collections.reduceIndexed",
    "kotlin.collections.reduceOrNull",
    "kotlin.collections.reduceIndexedOrNull",
    "kotlin.collections.runningFold",
    "kotlin.collections.runningFoldIndexed",
    "kotlin.collections.runningReduce",
    "kotlin.collections.runningReduceIndexed",
    "kotlin.collections.onEach",
    "kotlin.collections.onEachIndexed",
    "kotlin.collections.any",
    "kotlin.collections.all",
    "kotlin.collections.none",
    "kotlin.collections.zip",
    "kotlin.collections.windowed",
    "kotlin.collections.chunked",
    "kotlin.collections.joinToString",
    "kotlin.collections.count",
    "kotlin.collections.sumOf",
    "kotlin.sequences.forEach",
    "kotlin.sequences.associate",
    "kotlin.sequences.associateBy",
    "kotlin.sequences.associateWith",
    "kotlin.sequences.groupBy",
    "kotlin.sequences.fold",
    "kotlin.sequences.reduce",
    "kotlin.sequences.reduceOrNull",
    "kotlin.sequences.any",
    "kotlin.sequences.all",
    "kotlin.sequences.none",
    "kotlin.sequences.count",
    "kotlin.sequences.sumOf",
)

private val EagerComposeScopeFunctions = setOf(
    "androidx.compose.ui.text.buildAnnotatedString",
    "androidx.compose.ui.text.withAnnotation",
    "androidx.compose.ui.text.withLink",
    "androidx.compose.ui.text.withStyle",
)

private val EagerScopeFunctions = EagerStdlibScopeFunctions + EagerComposeScopeFunctions
