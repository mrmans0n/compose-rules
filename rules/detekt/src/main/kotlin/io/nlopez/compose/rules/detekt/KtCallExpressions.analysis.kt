// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression

@OptIn(KaExperimentalApi::class)
internal fun KtCallExpression.isResolvedCallToAnyOf(fqNames: Set<FqName>): Boolean = runCatching {
    analyze(this) {
        val call = this@isResolvedCallToAnyOf.resolveCall() as? KaFunctionCall<*> ?: return@analyze false
        call.signature.symbol.callableId?.asSingleFqName() in fqNames
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

@OptIn(KaExperimentalApi::class)
internal fun KtCallExpression.isResolvedCallToAnyNamed(fqNames: Set<String>): Boolean = runCatching {
    analyze(this) {
        val call = this@isResolvedCallToAnyNamed.resolveCall() as? KaFunctionCall<*> ?: return@analyze false
        call.signature.symbol.callableId?.asSingleFqName()?.asString() in fqNames
    }
}.getOrDefault(false)
