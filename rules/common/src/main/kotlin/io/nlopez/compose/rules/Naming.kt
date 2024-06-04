// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.hasReceiverType
import io.nlopez.compose.core.util.isOperator
import io.nlopez.compose.core.util.isSuppressed
import io.nlopez.compose.core.util.returnsValue
import org.jetbrains.kotlin.psi.KtFunction

class Naming : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        // If it's a block we can't know if there is a return type or not
        if (!function.hasBlockBody()) return

        // Operators have fixed names that we can't modify, so this rule is useless in that case
        if (function.isOperator) return

        // If it's suppressed by the official lints, we will honor that too
        if (function.isSuppressed("ComposableNaming")) return

        val functionName = function.name?.takeUnless(String::isEmpty) ?: return
        val firstLetter = functionName.first()

        if (function.returnsValue) {
            // If it returns value, the composable should start with a lowercase letter
            if (firstLetter.isUpperCase()) {
                // If it's allowed, we don't report it
                val isAllowed = config.getSet("allowedComposableFunctionNames", emptySet())
                    .any {
                        it.toRegex().matches(functionName)
                    }
                if (isAllowed) return
                emitter.report(function, ComposablesThatReturnResultsShouldBeLowercase)
            }
        } else {
            // If it returns Unit or doesn't have a return type, we should start with an uppercase letter
            // If the composable has a receiver, we can ignore this.
            if (firstLetter.isLowerCase() && !function.hasReceiverType) {
                emitter.report(function, ComposablesThatDoNotReturnResultsShouldBeCapitalized)
            }
        }
    }

    companion object {

        val ComposablesThatDoNotReturnResultsShouldBeCapitalized = """
            Composable functions that return Unit should start with an uppercase letter.
            They are considered declarative entities that can be either present or absent in a composition and therefore follow the naming rules for classes.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-composable-functions-properly for more information.
        """.trimIndent()

        val ComposablesThatReturnResultsShouldBeLowercase = """
            Composable functions that return a value should start with a lowercase letter.
            While useful and accepted outside of @Composable functions, this factory function convention has drawbacks that set inappropriate expectations for callers when used with @Composable functions.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-composable-functions-properly for more information.
        """.trimIndent()
    }
}
