// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.idea.references.mainReference
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
