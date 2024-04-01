// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.findDirectChildrenByClass
import io.nlopez.rules.core.util.isComposable
import io.nlopez.rules.core.util.isModifierReceiver
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReturnExpression

class ModifierComposed : ComposeKtVisitor {

    override fun visitFunction(function: KtFunction, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {
        if (!with(config) { function.isModifierReceiver }) return
        if (function.isComposable) return

        // If using a body expression, we can directly check for it being a call to `composed`
        val bodyExpression = function.bodyExpression
        if (bodyExpression is KtCallExpression && bodyExpression.calleeExpression?.text == "composed") {
            emitter.report(function, ComposedModifier)
        }

        // Otherwise, check the return statement expression
        val bodyBlockExpression = function.bodyBlockExpression ?: return
        val returnsComposed = bodyBlockExpression.findDirectChildrenByClass<KtReturnExpression>()
            .mapNotNull { it.returnedExpression }
            .filterIsInstance<KtCallExpression>()
            .any { it.calleeExpression?.text == "composed" }

        if (returnsComposed) {
            emitter.report(function, ComposedModifier)
        }
    }

    companion object {
        val ComposedModifier = """
            Using composed for modifiers is not recommended anymore, due to the performance issues it creates.
            You should consider migrating this modifier to be based on Modifier.Node instead.

            See https://mrmans0n.github.io/compose-rules/rules/#avoid-modifier-extension-factory-functions for more information.
        """.trimIndent()
    }
}
