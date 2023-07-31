// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.isModifier
import org.jetbrains.kotlin.psi.KtFunction

class ComposeModifierNaming : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        // If there is a modifier param, we bail
        val modifiers = function.valueParameters.filter { it.isModifier }

        // If there are no modifiers, or more than one, we don't care as much about the naming
        if (modifiers.count() != 1) return

        val modifier = modifiers.first()

        // In case we didn't find any `modifier` parameters, we check if it emits content and report the error if so.
        if (modifier.name != "modifier") {
            emitter.report(function, ModifiersAreSupposedToBeCalledLowercaseModifier)
        }
    }

    companion object {
        val ModifiersAreSupposedToBeCalledLowercaseModifier = """
            This @Composable has a single modifier, and its name is not `modifier`.

            The parameter should be called `modifier` as that is the convention expected for these types of parameters.

            See https://mrmans0n.github.io/compose-rules/rules/#TODO for more information.
        """.trimIndent()
    }
}
