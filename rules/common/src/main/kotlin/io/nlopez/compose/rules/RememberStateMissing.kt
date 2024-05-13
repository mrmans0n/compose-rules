// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.isRemembered
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
            .filter { it.calleeExpression?.text in MethodsThatNeedRemembering }
            // Only for those, we traverse up to [function], to see if it was actually remembered
            .filterNot { it.isRemembered(function) }
            // If it wasn't, we show the error
            .forEach { callExpression ->
                val errorMessage = MethodsAndErrorsThatNeedRemembering[callExpression.calleeExpression!!.text].orEmpty()
                emitter.report(callExpression, errorMessage, false)
            }
    }

    companion object {
        private val MethodsThatNeedRemembering = setOf(
            "derivedStateOf",
            "mutableStateOf",
            "mutableIntStateOf",
            "mutableFloatStateOf",
            "mutableDoubleStateOf",
            "mutableLongStateOf",
        )
        private val MethodsAndErrorsThatNeedRemembering = MethodsThatNeedRemembering.associateWith { errorMessage(it) }

        fun errorMessage(name: String): String = """
            Using `$name` in a @Composable function without it being inside of a remember function.
            If you don't remember the state instance, a new state instance will be created when the function is recomposed.

            See https://mrmans0n.github.io/compose-rules/rules/#state-should-be-remembered-in-composables for more information.
        """.trimIndent()
    }
}
