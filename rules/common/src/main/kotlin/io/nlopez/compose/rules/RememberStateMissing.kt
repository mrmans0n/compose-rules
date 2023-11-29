// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isRemembered
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction

class RememberStateMissing : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) {
        // To keep memory consumption in check, we first traverse down until we see one of our known functions
        // that need remembering
        function.findChildrenByClass<KtCallExpression>()
            .filter { MethodsThatNeedRemembering.contains(it.calleeExpression?.text) }
            // Only for those, we traverse up to [function], to see if it was actually remembered
            .filterNot { it.isRemembered(function) }
            // If it wasn't, we show the error
            .forEach { callExpression ->
                when (callExpression.calleeExpression!!.text) {
                    "mutableStateOf" -> emitter.report(callExpression, MutableStateOfNotRemembered, false)
                    "derivedStateOf" -> emitter.report(callExpression, DerivedStateOfNotRemembered, false)
                }
            }
    }

    companion object {
        private val MethodsThatNeedRemembering = setOf(
            "derivedStateOf",
            "mutableStateOf",
        )
        val DerivedStateOfNotRemembered = errorMessage("derivedStateOf")
        val MutableStateOfNotRemembered = errorMessage("mutableStateOf")

        private fun errorMessage(name: String): String = """
            Using `$name` in a @Composable function without it being inside of a remember function.
            If you don't remember the state instance, a new state instance will be created when the function is recomposed.

            See https://mrmans0n.github.io/compose-rules/rules/#state-should-be-remembered-in-composables for more information.
        """.trimIndent()
    }
}
