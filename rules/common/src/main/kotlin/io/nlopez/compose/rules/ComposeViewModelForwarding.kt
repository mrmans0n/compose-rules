// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig.Companion.config
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.definedInInterface
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isActual
import io.nlopez.rules.core.util.isOverride
import io.nlopez.rules.core.util.isRestartableEffect
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression

class ComposeViewModelForwarding : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        if (function.isOverride || function.definedInInterface || function.isActual) return
        val bodyBlock = function.bodyBlockExpression ?: return

        // We get here a list of variable names that tentatively contain ViewModels
        val parameters = function.valueParameterList?.parameters ?: emptyList()
        // Exit early to avoid hitting non-param composables
        if (parameters.isEmpty()) return

        val stateHolderValidNames = Regex(
            function.config()
                .getList("allowedStateHolderNames", defaultStateHolderNames)
                .ifEmpty { defaultStateHolderNames }
                .joinToString(
                    separator = "|",
                    prefix = "(",
                    postfix = ")",
                ),
        )

        val viewModelParameterNames = parameters.filter { parameter ->
            // We can't do much better than looking at the types at face value
            parameter.typeReference?.text?.matches(stateHolderValidNames) == true
        }
            .mapNotNull { it.name }
            .toSet()

        // We want now to see if these parameter names are used in any other calls to functions that start with
        // a capital letter (so, most likely, composables).
        bodyBlock.findChildrenByClass<KtCallExpression>()
            .filter { callExpression -> callExpression.calleeExpression?.text?.first()?.isUpperCase() ?: false }
            // Avoid LaunchedEffect/DisposableEffect/etc that can use the VM as a key
            .filterNot { callExpression -> callExpression.isRestartableEffect }
            .flatMap { callExpression ->
                // Get VALUE_ARGUMENT that has a REFERENCE_EXPRESSION. This would map to `viewModel` in this example:
                // MyComposable(viewModel, ...)
                callExpression.valueArguments
                    .mapNotNull { valueArgument -> valueArgument.getArgumentExpression() as? KtReferenceExpression }
                    .filter { reference -> reference.text in viewModelParameterNames }
                    .map { callExpression }
            }
            .forEach { callExpression ->
                emitter.report(callExpression, AvoidViewModelForwarding, false)
            }
    }

    companion object {
        private val defaultStateHolderNames = listOf(".*ViewModel", ".*Presenter")
        val AvoidViewModelForwarding = """
            Forwarding a ViewModel/Presenter through multiple @Composable functions should be avoided. Consider using state hoisting.

            See https://mrmans0n.github.io/compose-rules/rules/#hoist-all-the-things for more information.
        """.trimIndent()
    }
}
