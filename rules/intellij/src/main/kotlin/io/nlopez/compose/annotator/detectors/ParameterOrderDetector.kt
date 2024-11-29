// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator.detectors

import io.nlopez.compose.annotator.MODIFIER
import io.nlopez.compose.annotator.errors.ComposableError
import io.nlopez.compose.annotator.errors.ParameterOrderError
import io.nlopez.compose.annotator.fqNameString
import io.nlopez.compose.annotator.runIf
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtParameter

object ParameterOrderDetector : Detector {

    private val KtParameter.isModifier: Boolean
        get() = typeReference?.fqNameString == MODIFIER

    override fun invoke(composable: KtFunction): List<ComposableError> {
        // We need to make sure the proper order is respected. It should be:
        // 1. params without defaults
        // 2. modifiers
        // 3. params with defaults
        // 4. optional: lambda that can have no defaults

        // Let's try to build the ideal ordering first, and compare against that.
        val currentOrder = composable.valueParameters

        // We look in the original params without defaults and see if the last one is a function.
        val hasTrailingLambdaWithoutDefaults = composable.valueParameters
            .lastOrNull()
            ?.typeReference
            ?.typeElement is KtFunctionType

        val trailingLambda = if (hasTrailingLambdaWithoutDefaults) {
            listOf(composable.valueParameters.last())
        } else {
            emptyList()
        }

        val (withDefaults, withoutDefaults) = composable.valueParameters
            .runIf(hasTrailingLambdaWithoutDefaults) { dropLast(1) }
            .partition { it.hasDefaultValue() }

        val sortedWithDefaults = withDefaults.sortedWith(
            compareByDescending<KtParameter> { it.isModifier }.thenByDescending { it.name == "modifier" },
        )

        val idealOrdering = withoutDefaults + sortedWithDefaults + trailingLambda

        return if (composable.valueParameters == idealOrdering) {
            emptyList()
        } else {
            listOf(ParameterOrderError(composable, currentOrder, idealOrdering))
        }
    }
}
