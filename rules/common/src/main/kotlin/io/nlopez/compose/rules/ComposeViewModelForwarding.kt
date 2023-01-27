// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.definedInInterface
import io.nlopez.rules.core.util.findDirectChildrenByClass
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
        val viewModelParameterNames = parameters.filter { parameter ->
            // We can't do much better than this. We could look for viewModel() / weaverViewModel() but that
            // would give us way less (and less useful) hits.
            parameter.typeReference?.text?.endsWith("ViewModel") ?: false
        }
            .mapNotNull { it.name }
            .toSet()

        // We want now to see if these parameter names are used in any other calls to functions that start with
        // a capital letter (so, most likely, composables).
        bodyBlock.findDirectChildrenByClass<KtCallExpression>()
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
        val AvoidViewModelForwarding = """
            Forwarding a ViewModel through multiple @Composable functions should be avoided. Consider using state hoisting.

            See https://mrmans0n.github.io/compose-rules/rules/#hoist-all-the-things for more information.
        """.trimIndent()
    }
}
