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

class RememberContentMissing : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) {
        // To keep memory consumption in check, we first traverse down until we see one of our known functions
        // that need remembering
        function.findChildrenByClass<KtCallExpression>()
            .filter { it.calleeExpression?.text in ContentThatNeedsRemembering }
            // Only for those, we traverse up to [function], to see if it was actually remembered
            .filterNot { it.isRemembered(function) }
            // If it wasn't, we show the error
            .forEach { callExpression ->
                when (callExpression.calleeExpression!!.text) {
                    "movableContentOf" -> emitter.report(
                        element = callExpression,
                        errorMessage = MovableContentOfNotRemembered,
                        canBeAutoCorrected = false,
                    )

                    "movableContentWithReceiverOf" -> emitter.report(
                        element = callExpression,
                        errorMessage = MovableContentWithReceiverOfNotRemembered,
                        canBeAutoCorrected = false,
                    )
                }
            }
    }

    companion object {
        private val ContentThatNeedsRemembering = setOf(
            "movableContentOf",
            "movableContentWithReceiverOf",
        )

        val MovableContentOfNotRemembered = errorMessage("movableContentOf")
        val MovableContentWithReceiverOfNotRemembered = errorMessage("movableContentWithReceiverOf")
        private fun errorMessage(name: String): String = """
            Using `$name` in a @Composable function without it being remembered can cause visual problems, as the content would be recycled when detached from the composition.

            See https://mrmans0n.github.io/compose-rules/rules/#movable-content-should-be-remembered for more information.
        """.trimIndent()
    }
}
