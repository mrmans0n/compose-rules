// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(KaExperimentalApi::class)

package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.resolveCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression

internal fun KtCallExpression.isResolvedCallToAnyOf(fqNames: Set<FqName>): Boolean = runCatching {
    analyze(this) {
        val call = this@isResolvedCallToAnyOf.resolveCall() as? KaFunctionCall<*> ?: return@analyze false
        call.signature.symbol.callableId?.asSingleFqName() in fqNames
    }
}.getOrDefault(false)

internal fun KtCallExpression.isComposableCall(): Boolean = runCatching {
    analyze(this) {
        val call = this@isComposableCall.resolveCall() as? KaFunctionCall<*> ?: return@analyze false
        call.signature.symbol.hasComposableAnnotation() ||
            calleeExpression?.expressionType?.hasComposableAnnotation() == true
    }
}.getOrDefault(false)

internal fun KtCallExpression.isReadOnlyComposableCall(): Boolean = runCatching {
    analyze(this) {
        val call = this@isReadOnlyComposableCall.resolveCall() as? KaFunctionCall<*> ?: return@analyze false
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
        val call = this@isResolvedCallToAnyNamed.resolveCall() as? KaFunctionCall<*> ?: return@analyze false
        call.signature.symbol.callableId?.asSingleFqName()?.asString() in fqNames
    }
}.getOrDefault(false)
