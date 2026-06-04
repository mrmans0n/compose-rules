// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.components.expectedType
import org.jetbrains.kotlin.analysis.api.components.functionType
import org.jetbrains.kotlin.analysis.api.components.type
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtTypeReference
import io.nlopez.compose.core.util.isComposable as hasComposableAnnotationText

internal fun KtElement.isInsideComposableScope(): Boolean {
    var current = parent
    while (current != null) {
        when (current) {
            is KtNamedFunction -> return current.isComposable()
            is KtPropertyAccessor -> return current.isComposable()
            is KtLambdaExpression -> if (current.isComposable()) return true
        }
        current = current.parent
    }
    return false
}

internal fun KtNamedFunction.isComposable(): Boolean = hasComposableAnnotationText ||
    runCatching {
        analyze(this) {
            this@isComposable.symbol.hasComposableAnnotation()
        }
    }.getOrDefault(false)

internal fun KtPropertyAccessor.isComposable(): Boolean = hasComposableAnnotationText ||
    runCatching {
        analyze(this) {
            this@isComposable.symbol.hasComposableAnnotation()
        }
    }.getOrDefault(false)

private fun KtLambdaExpression.isComposable(): Boolean = runCatching {
    analyze(this) {
        functionLiteral.functionType.hasComposableAnnotation() ||
            this@isComposable.expectedType?.hasComposableAnnotation() == true
    }
}.getOrDefault(false)

internal fun KtNamedFunction.isReadOnlyComposable(): Boolean = runCatching {
    analyze(this) {
        this@isReadOnlyComposable.symbol.hasReadOnlyComposableAnnotation()
    }
}.getOrDefault(false)

internal fun KtPropertyAccessor.isReadOnlyComposable(): Boolean = runCatching {
    analyze(this) {
        this@isReadOnlyComposable.symbol.hasReadOnlyComposableAnnotation()
    }
}.getOrDefault(false)

internal fun KtParameter.hasComposableType(): Boolean = typeReference?.hasComposableType() == true

internal fun KtTypeReference.hasComposableType(): Boolean = text.contains("@Composable") ||
    runCatching {
        analyze(this) {
            type.hasComposableAnnotation()
        }
    }.getOrDefault(false)

internal fun KaAnnotatedSymbol.hasComposableAnnotation(): Boolean =
    annotations.any { it.classId == ClassId.topLevel(ComposeFqNames.Composable) }

internal fun KaAnnotated.hasComposableAnnotation(): Boolean =
    annotations.any { it.classId == ClassId.topLevel(ComposeFqNames.Composable) }

internal fun KaAnnotatedSymbol.hasReadOnlyComposableAnnotation(): Boolean =
    annotations.any { it.classId == ClassId.topLevel(ComposeFqNames.ReadOnlyComposable) }
