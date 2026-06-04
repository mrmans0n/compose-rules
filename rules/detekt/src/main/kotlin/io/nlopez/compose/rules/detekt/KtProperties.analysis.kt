// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtProperty

internal fun KtProperty.hasComposeStateDelegate(): Boolean = runCatching {
    val delegate = delegateExpression ?: return@runCatching false

    analyze(delegate) {
        val delegateType = delegate.expressionType ?: return@analyze false
        delegateType.symbol?.classId?.asSingleFqName() in ComposeStateFqNames ||
            delegateType.allSupertypes.any { supertype ->
                supertype.symbol?.classId?.asSingleFqName() in ComposeStateFqNames
            }
    }
}.getOrDefault(false)

private val ComposeStateFqNames: Set<FqName> = setOf(
    ComposeFqNames.State,
    ComposeFqNames.MutableState,
    ComposeFqNames.IntState,
    ComposeFqNames.MutableIntState,
    ComposeFqNames.LongState,
    ComposeFqNames.MutableLongState,
    ComposeFqNames.FloatState,
    ComposeFqNames.MutableFloatState,
    ComposeFqNames.DoubleState,
    ComposeFqNames.MutableDoubleState,
)
