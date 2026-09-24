// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(KaExperimentalApi::class)

package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.fakeOverrideOriginal
import org.jetbrains.kotlin.analysis.api.components.resolveCall
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
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

/**
 * Whether this is a call to a library function that gets no lambda, anonymous function or callable reference, so it
 * cannot run code from the analyzed sources.
 */
internal fun KtCallExpression.isLambdaLessLibraryCall(): Boolean {
    val hasFunctionArgument = lambdaArguments.isNotEmpty() ||
        valueArguments.any { argument ->
            when (argument.getArgumentExpression()?.unwrapArgumentExpression()) {
                is KtLambdaExpression, is KtNamedFunction, is KtCallableReferenceExpression -> true
                else -> false
            }
        }
    if (hasFunctionArgument) return false

    return runCatching {
        analyze(this) {
            val call = this@isLambdaLessLibraryCall.resolveCall() ?: return@analyze false
            call.dispatchReceiver?.type !is KaFunctionType && isDeclaredInLibrary(call.signature.symbol)
        }
    }.getOrDefault(false)
}

internal fun KaSession.isDeclaredInLibrary(symbol: KaCallableSymbol): Boolean =
    symbol.fakeOverrideOriginal.origin in LibraryOrigins

private val LibraryOrigins = setOf(KaSymbolOrigin.LIBRARY, KaSymbolOrigin.JAVA_LIBRARY)

internal fun KtCallExpression.hasExplicitArgumentMappedToAny(parameterNames: Set<String>): Boolean = runCatching {
    analyze(this) {
        val call = this@hasExplicitArgumentMappedToAny.resolveCall() ?: return@analyze false
        call.valueArgumentMapping.values.any { parameter ->
            parameter.symbol.name.asString() in parameterNames
        }
    }
}.getOrDefault(false)

internal fun KtCallExpression.lambdaArgumentMappedTo(parameterName: String): KtLambdaExpression? {
    val lambdaExpressions = valueArguments.mapNotNull { argument ->
        argument.getArgumentExpression()?.unwrapArgumentExpression() as? KtLambdaExpression
    } + lambdaArguments.mapNotNull { argument -> argument.getLambdaExpression() }
    if (lambdaExpressions.isEmpty()) return null

    return runCatching {
        analyze(this) {
            val call = this@lambdaArgumentMappedTo.resolveCall() ?: return@analyze null
            lambdaExpressions.firstOrNull { lambdaExpression ->
                call.valueArgumentMapping.entries
                    .firstOrNull { (argument, _) -> argument.unwrapArgumentExpression() == lambdaExpression }
                    ?.value
                    ?.symbol
                    ?.name
                    ?.asString() == parameterName
            }
        }
    }.getOrNull()
}

internal tailrec fun KtExpression.unwrapArgumentExpression(): KtExpression = when (this) {
    is KtAnnotatedExpression -> baseExpression?.unwrapArgumentExpression() ?: this
    is KtLabeledExpression -> baseExpression?.unwrapArgumentExpression() ?: this
    is KtParenthesizedExpression -> expression?.unwrapArgumentExpression() ?: this
    else -> this
}
