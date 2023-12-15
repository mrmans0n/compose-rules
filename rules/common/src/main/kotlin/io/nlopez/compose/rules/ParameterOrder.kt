// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isComposable
import io.nlopez.rules.core.util.isModifier
import io.nlopez.rules.core.util.runIf
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtUserType

class ParameterOrder : ComposeKtVisitor {

    override fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {
        val lambdaTypes = mutableSetOf<String>()
        // Add types defined in the config
        lambdaTypes += config.getSet("treatAsLambda", emptySet())

        // Add fun interfaces
        lambdaTypes += file.findChildrenByClass<KtClass>()
            .filter { it.isInterface() && it.hasModifier(KtTokens.FUN_KEYWORD) }
            .mapNotNull { it.name }

        fun KtTypeElement.isLambda(): Boolean = when (this) {
            is KtFunctionType -> true
            is KtNullableType -> innerType?.isLambda() == true
            is KtUserType -> getReferencedName() in lambdaTypes
            else -> false
        }

        // Add typealias with functional types
        // NOTE: it has to be last, so that isLambda picks up fun interfaces / config stuff in lambdaTypes
        lambdaTypes += file.findChildrenByClass<KtTypeAlias>()
            .filter { it.getTypeReference()?.typeElement?.isLambda() == true }
            .mapNotNull { it.name }
            .toSet()

        val composables = file.findChildrenByClass<KtFunction>()
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
                ?.typeElement
                ?.isLambda() == true

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
                compareByDescending<KtParameter> { with(config) { it.isModifier } }
                    .thenByDescending { it.name == "modifier" },
            )

            // We create our ideal ordering of params for the ideal composable.
            val properOrder = withoutDefaults + sortedWithDefaults + trailingLambda

            // If it's not the same as the current order, we show the rule violation.
            if (currentOrder != properOrder) {
                emitter.report(function, createErrorMessage(currentOrder, properOrder))
            }
        }
    }

    companion object {
        fun createErrorMessage(currentOrder: List<KtParameter>, properOrder: List<KtParameter>): String =
            createErrorMessage(currentOrder.joinToString { it.text }, properOrder.joinToString { it.text })

        fun createErrorMessage(currentOrder: String, properOrder: String): String = """
            Parameters in a composable function should be ordered following this pattern: params without defaults, modifiers, params with defaults and optionally, a trailing function that might not have a default param.
            Current params are: [$currentOrder] but should be [$properOrder].

            See https://mrmans0n.github.io/compose-rules/rules/#ordering-composable-parameters-properly for more information.
        """.trimIndent()
    }
}
