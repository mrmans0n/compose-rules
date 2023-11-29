// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.isModifier
import org.jetbrains.kotlin.psi.KtFunction

class ModifierNaming : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) {
        // If there is a modifier param, we bail
        val modifiers = function.valueParameters.filter { it.isModifier }

        // If there are no modifiers, or more than one, we don't care as much about the naming
        if (modifiers.isEmpty()) return

        val count = modifiers.size
        if (count == 1) {
            if (modifiers.first().name?.equals("modifier") != true) {
                emitter.report(modifiers.first(), ModifiersAreSupposedToBeCalledModifierWhenAlone)
                return
            }
        } else {
            for (modifier in modifiers) {
                val valid = modifier.name?.lowercase()?.endsWith("modifier") ?: false
                if (!valid) {
                    emitter.report(modifier, ModifiersAreSupposedToEndInModifierWhenMultiple)
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
