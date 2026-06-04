// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.components.expectedType
import org.jetbrains.kotlin.analysis.api.components.functionType
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
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

private fun KtNamedFunction.isComposable(): Boolean = hasComposableAnnotationText ||
    runCatching {
        analyze(this) {
            this@isComposable.symbol.hasComposableAnnotation()
        }
    }.getOrDefault(false)

private fun KtPropertyAccessor.isComposable(): Boolean = hasComposableAnnotationText ||
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

private fun KaAnnotatedSymbol.hasComposableAnnotation(): Boolean =
    annotations.any { it.classId == ClassId.topLevel(ComposeFqNames.Composable) }

private fun KaAnnotated.hasComposableAnnotation(): Boolean =
    annotations.any { it.classId == ClassId.topLevel(ComposeFqNames.Composable) }
