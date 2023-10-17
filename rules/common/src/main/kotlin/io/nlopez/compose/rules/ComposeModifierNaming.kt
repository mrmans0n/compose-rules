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
        if (modifiers.isEmpty()) return

        val count = modifiers.size

        modifiers.forEachIndexed { index, modifier ->
            val isCorrectSingleModifier = modifier.name?.equals("modifier") ?: false
            val valid = if (index == 0) {
                isCorrectSingleModifier
            } else {
                !isCorrectSingleModifier && modifier.name?.lowercase()?.endsWith("modifier") ?: false
            }
            when {
                !valid && count == 1 -> emitter.report(modifier, ModifiersAreSupposedToBeCalledModifierWhenAlone)
                !valid -> emitter.report(modifier, ModifiersAreSupposedToEndInModifierWhenMultiple)
                else -> {
                    // no-op
                }
            }
        }
    }

    companion object {
        val ModifiersAreSupposedToBeCalledModifierWhenAlone = """
            Modifier parameters should be called `modifier`.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-modifiers-properly for more information.
        """.trimIndent()
        val ModifiersAreSupposedToEndInModifierWhenMultiple = """
            Modifier parameters should be called `modifier` or end in `Modifier` if there are more than one in the same @Composable.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-modifiers-properly for more information.
        """.trimIndent()
    }
}
