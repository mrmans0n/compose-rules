// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.emitsContent
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.isLambda
import io.nlopez.compose.core.util.modifierParameter
import org.jetbrains.kotlin.psi.KtFunction

class LambdaParameterEventTrailing : ComposeKtVisitor {
    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        // We want this rule to only run for functions that emit content
        if (!function.emitsContent(config)) return

        // We'd also want for it to have a modifier to apply the rule, which serves two purposes:
        // - making sure that it's the separation between required and optional parameters
        // - the lambda would be able to be moved before the modifier and not be the trailing one
        if (function.modifierParameter(config) == null) return

        val trailingParam = function.valueParameters.lastOrNull() ?: return

        // Check if the trailing element...
        // - is a lambda
        // - is not @Composable
        // - doesn't have a default value
        // - starts with "on", meaning it's an event
        val typeReference = trailingParam.typeReference ?: return
        if (!typeReference.isLambda()) return
        if (typeReference.isComposable) return
        if (trailingParam.hasDefaultValue()) return
        val name = trailingParam.name ?: return
        if (!name.startsWith("on")) return

        emitter.report(trailingParam, EventLambdaIsTrailingLambda)
    }

    companion object {
        val EventLambdaIsTrailingLambda = """
            Lambda parameters in a @Composable that are for events (e.g. onClick, onChange, etc) and are required (they don't have a default value) should not be used as the trailing parameter.

            Composable functions that emit content usually reserve the trailing lambda syntax for the content slot, and that can lead to an assumption that other composables can be used in that lambda.

            See https://mrmans0n.github.io/compose-rules/rules/#TODO for more information.
        """.trimIndent()
    }
}
