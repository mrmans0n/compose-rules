// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.definedInInterface
import io.nlopez.rules.core.util.isAbstract
import io.nlopez.rules.core.util.isActual
import io.nlopez.rules.core.util.isModifier
import io.nlopez.rules.core.util.isOverride
import io.nlopez.rules.core.util.lastChildLeafOrSelf
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtFunction

class ComposeModifierWithoutDefault : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        if (function.definedInInterface || function.isActual || function.isOverride || function.isAbstract) return

        // Look for modifier params in the composable signature, and if any without a default value is found, error out.
        function.valueParameters.filter { it.isModifier }
            .filterNot { it.hasDefaultValue() }
            .forEach { modifierParameter ->
                emitter.report(modifierParameter, MissingModifierDefaultParam, true)

                // This error is easily auto fixable, we just inject ` = Modifier` to the param
                if (autoCorrect) {
                    val lastToken = modifierParameter.node.lastChildLeafOrSelf() as LeafPsiElement
                    val currentText = lastToken.text
                    lastToken.rawReplaceWithText("$currentText = Modifier")
                }
            }
    }

    companion object {
        val MissingModifierDefaultParam = """
            This @Composable function has a modifier parameter but it doesn't have a default value.

            See https://twitter.github.io/compose-rules/rules/#modifiers-should-have-default-parameters for more information.
        """.trimIndent()
    }
}
