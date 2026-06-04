// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

internal fun KtDotQualifiedExpression.isCompositionLocalCurrentRead(): Boolean = runCatching {
    val selector = selectorExpression as? KtNameReferenceExpression ?: return@runCatching false
    if (selector.getReferencedName() != "current") return@runCatching false

    analyze(receiverExpression) {
        val type = receiverExpression.expressionType ?: return@analyze false
        type.symbol?.classId?.asSingleFqName() == ComposeFqNames.CompositionLocal ||
            type.allSupertypes.any { supertype ->
                supertype.symbol?.classId?.asSingleFqName() == ComposeFqNames.CompositionLocal
            }
    }
}.getOrDefault(false)

internal fun KtQualifiedExpression.isComposeStateValueRead(): Boolean = runCatching {
    val selector = selectorExpression as? KtNameReferenceExpression ?: return@runCatching false
    val stateFqNames = ComposeStateValueFqNames[selector.getReferencedName()] ?: return@runCatching false

    analyze(receiverExpression) {
        val type = receiverExpression.expressionType ?: return@analyze false
        type.symbol?.classId?.asSingleFqName() in stateFqNames ||
            type.allSupertypes.any { supertype ->
                supertype.symbol?.classId?.asSingleFqName() in stateFqNames
            }
    }
}.getOrDefault(false)

private val ComposeStateValueFqNames = mapOf(
    "value" to setOf(ComposeFqNames.State, ComposeFqNames.MutableState),
    "intValue" to setOf(ComposeFqNames.IntState, ComposeFqNames.MutableIntState),
    "longValue" to setOf(ComposeFqNames.LongState, ComposeFqNames.MutableLongState),
    "floatValue" to setOf(ComposeFqNames.FloatState, ComposeFqNames.MutableFloatState),
    "doubleValue" to setOf(ComposeFqNames.DoubleState, ComposeFqNames.MutableDoubleState),
)
