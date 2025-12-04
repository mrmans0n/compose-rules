// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.composableLambdaTypes
import io.nlopez.compose.core.util.findAllChildrenByClass
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.isComposableLambda
import io.nlopez.compose.core.util.isLambda
import io.nlopez.compose.core.util.isModifier
import io.nlopez.compose.core.util.lambdaTypes
import io.nlopez.compose.core.util.runIf
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter

class ParameterOrder : ComposeKtVisitor {

    override fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {
        val lambdaTypes = file.lambdaTypes(config)
        val composableLambdaTypes = file.composableLambdaTypes(config)

        val composables = file.findAllChildrenByClass<KtFunction>()
            .filter { it.isComposable }

        for (function in composables) {
            // We need to make sure the proper order is respected. It should be:
            // 1. params without defaults
            // 2. modifiers
            // 3. params with defaults
            // 4. optional: function that might have no default

            // Let's try to build the ideal ordering first, and compare against that.
            val currentOrder = function.valueParameters

            // We look in the original params without defaults and see if the last one is a function.
            val hasTrailingFunction = function.valueParameters.lastOrNull()
                ?.typeReference
                ?.isLambda(treatAsLambdaTypes = lambdaTypes) == true

            val trailingLambda = if (hasTrailingFunction) {
                listOf(function.valueParameters.last())
            } else {
                emptyList()
            }

            // We extract the params without with and without defaults, and keep the order between them
            val (withDefaults, withoutDefaults) = function.valueParameters
                .runIf(hasTrailingFunction) { dropLast(1) }
                .partition { it.hasDefaultValue() }

            // As ComposeModifierMissingCheck will catch modifiers without a Modifier default, we don't have to care
            // about that case. We will sort the params with defaults so that the modifier(s) go first.
            val sortedWithDefaults = withDefaults.sortedWith(
                compareByDescending<KtParameter> { it.isModifier(config) }
                    .thenByDescending { it.name == "modifier" },
            )

            // We create our ideal ordering of params for the ideal composable.
            val properOrder = withoutDefaults + sortedWithDefaults + trailingLambda

            // Special case: If the proper order would move a NON-COMPOSABLE lambda with a default value to the
            // last position (making it a trailing lambda) and there's no existing trailing lambda, we should be
            // lenient. This avoids forcing non-composable lambdas with defaults to become trailing lambdas, which
            // is undesirable in Compose. However, composable lambdas (like content slots) are perfectly fine as
            // trailing lambdas, so we should still report those.
            val shouldBeLenient = !hasTrailingFunction &&
                properOrder.lastOrNull()?.let { lastParam ->
                    val typeRef = lastParam.typeReference
                    lastParam.hasDefaultValue() &&
                        typeRef?.isLambda(treatAsLambdaTypes = lambdaTypes) == true &&
                        typeRef.isComposableLambda(
                            treatAsLambdaTypes = lambdaTypes,
                            treatAsComposableLambdaTypes = composableLambdaTypes,
                        ).not()
                } == true

            // If it's not the same as the current order, we show the rule violation.
            if (!shouldBeLenient && currentOrder != properOrder) {
                emitter.report(function, createErrorMessage(currentOrder, properOrder))
            }
        }
    }

    companion object {
        fun createErrorMessage(currentOrder: List<KtParameter>, properOrder: List<KtParameter>): String =
            createErrorMessage(currentOrder.joinToString { it.text }, properOrder.joinToString { it.text })

        fun createErrorMessage(currentOrder: String, properOrder: String): String = """
            Parameters in a composable function should be ordered following this pattern: params without defaults, modifiers, params with defaults and optionally, a trailing function that might not have a default param.
            Current params are: [$currentOrder] but could be [$properOrder].
            See https://mrmans0n.github.io/compose-rules/rules/#ordering-composable-parameters-properly for more information.
        """.trimIndent()
    }
}
