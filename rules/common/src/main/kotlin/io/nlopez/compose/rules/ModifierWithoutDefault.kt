// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.ifFix
import io.nlopez.compose.core.util.definedInInterface
import io.nlopez.compose.core.util.isAbstract
import io.nlopez.compose.core.util.isActual
import io.nlopez.compose.core.util.isModifier
import io.nlopez.compose.core.util.isModifierReceiver
import io.nlopez.compose.core.util.isOpen
import io.nlopez.compose.core.util.isOverride
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory

class ModifierWithoutDefault : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        if (
            function.definedInInterface ||
            function.isActual ||
            function.isOverride ||
            function.isAbstract ||
            function.isOpen ||
            function.isModifierReceiver(config)
        ) {
            return
        }

        // Look for modifier params in the composable signature, and if any without a default value is found, error out.
        function.valueParameters.filter { it.isModifier(config) }
            .filterNot { it.hasDefaultValue() }
            .forEach { modifierParameter ->
                emitter.report(modifierParameter, MissingModifierDefaultParam, true).ifFix {
                    // This error is easily auto fixable, we just inject ` = Modifier` to the param. It needs to
                    // be a re-parsed parameter and not a text patch of the last leaf: that leaf is the identifier
                    // of the `Modifier` type reference, so patching it would leave behind an unresolvable
                    // `Modifier = Modifier` identifier and rules like ktlint's no-unused-imports would then
                    // consider the Modifier import unused and remove it.
                    val factory = KtPsiFactory.contextual(modifierParameter)
                    val newParameter = factory.createParameter("${modifierParameter.text} = Modifier")
                    modifierParameter.node.treeParent.replaceChild(modifierParameter.node, newParameter.node)
                }
            }
    }

    companion object {
        val MissingModifierDefaultParam = """
            This @Composable function has a modifier parameter but it doesn't have a default value.
            See https://mrmans0n.github.io/compose-rules/rules/#modifiers-should-have-default-parameters for more information.
        """.trimIndent()
    }
}
