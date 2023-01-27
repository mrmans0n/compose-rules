// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.isModifierReceiver
import org.jetbrains.kotlin.psi.KtFunction

class ComposeModifierComposable : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        if (!function.isModifierReceiver) return

        emitter.report(function, ComposableModifier)
    }

    companion object {
        val ComposableModifier = """
            Using @Composable builder functions for modifiers is not recommended, as they cause unnecessary recompositions.
            You should use Modifier.composed { ... } instead, as it limits recomposition to just the modifier instance, rather than the whole function tree.

            See https://twitter.github.io/compose-rules/rules/#avoid-modifier-extension-factory-functions for more information.
        """.trimIndent()
    }
}
