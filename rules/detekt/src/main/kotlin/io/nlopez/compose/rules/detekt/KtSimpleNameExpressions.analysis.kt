// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal fun KtSimpleNameExpression.isResolvedReadOf(property: KtProperty): Boolean = runCatching {
    analyze(this) {
        val propertySymbol = property.symbol
        val referenceSymbol = this@isResolvedReadOf.mainReference.resolveToSymbol()
        if (referenceSymbol == propertySymbol || referenceSymbol?.sourcePsiSafe<KtProperty>() == property) {
            return@analyze true
        }

        val read = this@isResolvedReadOf.resolveToCall() as? KaVariableAccessCall ?: return@analyze false
        read.signature.symbol == propertySymbol || read.signature.symbol.sourcePsiSafe<KtProperty>() == property
    }
}.getOrDefault(false)

internal fun KtSimpleNameExpression.resolvedPropertyInitializer(): KtExpression? =
    resolvedLocalImmutableProperty()?.initializer

internal fun KtSimpleNameExpression.resolvedLocalImmutableProperty(): KtProperty? = runCatching {
    analyze(this) {
        mainReference.resolveToSymbol()
            ?.sourcePsiSafe<KtProperty>()
            ?.takeIf { property -> property.isLocal && !property.isVar }
    }
}.getOrNull()

internal fun KtSimpleNameExpression.isComposablePropertyRead(): Boolean = resolvedPropertyRead(
    symbolPredicate = { property -> property.getter?.hasComposableAnnotation() == true },
    sourcePredicate = { property -> property.getter?.isComposable() == true },
)

internal fun KtSimpleNameExpression.isReadOnlyComposablePropertyRead(): Boolean = resolvedPropertyRead(
    symbolPredicate = { property -> property.getter?.hasReadOnlyComposableAnnotation() == true },
    sourcePredicate = { property -> property.getter?.isReadOnlyComposable() == true },
)

internal fun KtSimpleNameExpression.isPropertyReadWithExecutableAccess(): Boolean = resolvedPropertyRead(
    symbolPredicate = { false },
    sourcePredicate = { property -> property.getter?.bodyExpression != null || property.hasDelegate() },
)

internal fun KtSimpleNameExpression.isDelegatedComposeStateRead(): Boolean = runCatching {
    if (isPlainAssignmentLeftHandSide()) return@runCatching false

    analyze(this) {
        val property = ((resolveToCall() as? KaVariableAccessCall)?.signature?.symbol as? KaPropertySymbol)
            ?.sourcePsiSafe<KtProperty>()
            ?: mainReference.resolveToSymbol()?.sourcePsiSafe<KtProperty>()
            ?: return@analyze false
        val delegateExpression = property.delegateExpression ?: return@analyze false
        val delegateType = delegateExpression.expressionType ?: return@analyze false
        delegateType.symbol?.classId?.asSingleFqName() in ComposeStateFqNames ||
            delegateType.allSupertypes.any { supertype ->
                supertype.symbol?.classId?.asSingleFqName() in ComposeStateFqNames
            }
    }
}.getOrDefault(false)

private fun KtSimpleNameExpression.resolvedPropertyRead(
    symbolPredicate: (KaPropertySymbol) -> Boolean,
    sourcePredicate: (KtProperty) -> Boolean,
): Boolean = runCatching {
    if ((parent as? KtDotQualifiedExpression)?.receiverExpression == this) return@runCatching false

    analyze(this) {
        val property = ((resolveToCall() as? KaVariableAccessCall)?.signature?.symbol as? KaPropertySymbol)
            ?: mainReference.resolveToSymbol() as? KaPropertySymbol
            ?: return@analyze false
        symbolPredicate(property) || property.sourcePsiSafe<KtProperty>()?.let(sourcePredicate) == true
    }
}.getOrDefault(false)

private val ComposeStateFqNames = setOf(ComposeFqNames.State, ComposeFqNames.MutableState)

private fun KtSimpleNameExpression.isPlainAssignmentLeftHandSide(): Boolean {
    val assignedExpression = (parent as? KtDotQualifiedExpression)
        ?.takeIf { expression -> expression.selectorExpression == this }
        ?: this
    return (assignedExpression.parent as? KtBinaryExpression)?.let { expression ->
        expression.operationToken == KtTokens.EQ && expression.left == assignedExpression
    } == true
}
