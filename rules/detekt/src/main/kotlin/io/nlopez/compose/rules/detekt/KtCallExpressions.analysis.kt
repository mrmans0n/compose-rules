// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(KaExperimentalApi::class)

package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.resolveCall
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression

internal fun KtCallExpression.isResolvedCallToAnyOf(fqNames: Set<FqName>): Boolean = runCatching {
    analyze(this) {
        val call = this@isResolvedCallToAnyOf.resolveCall() ?: return@analyze false
        call.signature.symbol.callableId?.asSingleFqName() in fqNames
    }
}.getOrDefault(false)

internal fun KtCallExpression.isComposableCall(): Boolean = runCatching {
    analyze(this) {
        val call = this@isComposableCall.resolveCall() ?: return@analyze false
        call.signature.symbol.hasComposableAnnotation() ||
            calleeExpression?.expressionType?.hasComposableAnnotation() == true
    }
}.getOrDefault(false)

internal fun KtCallExpression.isReadOnlyComposableCall(): Boolean = runCatching {
    analyze(this) {
        val call = this@isReadOnlyComposableCall.resolveCall() ?: return@analyze false
        call.signature.symbol.hasReadOnlyComposableAnnotation()
    }
}.getOrDefault(false)

internal fun KtCallExpression.isRememberUpdatedStateCall(): Boolean =
    isResolvedCallToAnyOf(setOf(ComposeFqNames.RememberUpdatedState))

internal fun KtCallExpression.isMemoizingComposableCall(): Boolean = isResolvedCallToAnyOf(
    setOf(
        ComposeFqNames.Remember,
        ComposeFqNames.RememberSaveable,
        ComposeFqNames.Retain,
    ),
)

internal fun KtCallExpression.isResolvedCallToAnyNamed(fqNames: Set<String>): Boolean = runCatching {
    analyze(this) {
        val call = this@isResolvedCallToAnyNamed.resolveCall() ?: return@analyze false
        call.signature.symbol.callableId?.asSingleFqName()?.asString() in fqNames
    }
}.getOrDefault(false)

internal fun KtCallExpression.isResolvedInlineArgument(argumentExpression: KtExpression): Boolean = runCatching {
    analyze(this) {
        val call = this@isResolvedInlineArgument.resolveCall() ?: return@analyze false
        val function = call.signature.symbol
        if (function !is KaNamedFunctionSymbol || !function.isInline) return@analyze false

        val parameter = call.valueArgumentMapping.entries
            .firstOrNull { (argument, _) -> argument.unwrapArgumentExpression() == argumentExpression }
            ?.value
            ?.symbol
            ?: return@analyze false

        !parameter.isNoinline && !parameter.isCrossinline
    }
}.getOrDefault(false)

internal fun KtCallExpression.hasExplicitArgumentMappedToAny(parameterNames: Set<String>): Boolean = runCatching {
    analyze(this) {
        val call = this@hasExplicitArgumentMappedToAny.resolveCall() ?: return@analyze false
        call.valueArgumentMapping.values.any { parameter ->
            parameter.symbol.name.asString() in parameterNames
        }
    }
}.getOrDefault(false)

private tailrec fun KtExpression.unwrapArgumentExpression(): KtExpression = when (this) {
    is KtAnnotatedExpression -> baseExpression?.unwrapArgumentExpression() ?: this
    is KtLabeledExpression -> baseExpression?.unwrapArgumentExpression() ?: this
    is KtParenthesizedExpression -> expression?.unwrapArgumentExpression() ?: this
    else -> this
}
