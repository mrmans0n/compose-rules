// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.definedInInterface
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.findDirectChildrenByClass
import io.nlopez.rules.core.util.isActual
import io.nlopez.rules.core.util.isOverride
import io.nlopez.rules.core.util.isRestartableEffect
import io.nlopez.rules.core.util.joinToRegex
import io.nlopez.rules.core.util.joinToRegexOrNull
import io.nlopez.rules.core.util.runIfNotNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression

class ViewModelForwarding : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) {
        if (function.isOverride || function.definedInInterface || function.isActual) return
        val bodyBlock = function.bodyBlockExpression ?: return

        // We get here a list of variable names that tentatively contain ViewModels
        val parameters = function.valueParameterList?.parameters ?: emptyList()
        // Exit early to avoid hitting non-param composables
        if (parameters.isEmpty()) return

        val stateHolderValidNames = (config.getSet("allowedStateHolderNames", emptySet()) + defaultStateHolderNames)
            .joinToRegex()

        val allowedForwardingTargetNames = config.getSet("allowedForwarding", emptySet()).joinToRegexOrNull()
        val allowedForwardingOfTypes = config.getSet("allowedForwardingOfTypes", emptySet()).joinToRegexOrNull()

        // Grab all the types we are going to watch if they are forwarded
        val viewModelParameterNames = parameters
            .filter { parameter ->
                // We can't do much better than looking at the types at face value
                parameter.typeReference?.text?.matches(stateHolderValidNames) == true
            }
            // Filter out the ones we are cool with forwarding
            .runIfNotNull(allowedForwardingOfTypes) { regex ->
                filterNot { parameter ->
                    parameter.typeReference?.text?.matches(regex) == true
                }
            }
            .mapNotNull { it.name }
            .toSet()

        val checkedCallExpressions = mutableSetOf<KtCallExpression>()
        fun checkCallExpressions(
            callExpressions: Sequence<KtCallExpression>,
            scopedParameter: String? = null,
            usesItObjectRef: Boolean = false,
        ) {
            // Filter call expressions that are scope functions
            callExpressions
                .filter { callExpression -> callExpression.isScopeFunction }
                .forEach { callExpression ->
                    // For each scope function, get the lambda arguments
                    callExpression.lambdaArguments
                        .mapNotNull { it.getLambdaExpression()?.bodyExpression }
                        .forEach { lambdaBodyExpression ->
                            // For each lambda body expression, get the call expressions
                            // ensures that only the immediate children of the bodyExpression that are instances of
                            // KtCallExpression are checked, effectively limiting the scope to the first level of nesting.
                            lambdaBodyExpression.findDirectChildrenByClass<KtCallExpression>()
                                .filterNot { it in checkedCallExpressions }
                                .also { expressions ->
                                    checkCallExpressions(
                                        scopedParameter = callExpression.getScopedParameterValue(),
                                        usesItObjectRef = callExpression.hasItObjectReference,
                                        callExpressions = expressions,
                                    )
                                }
                        }
                }

            callExpressions
                .filter { callExpression -> callExpression.calleeExpression?.text?.first()?.isUpperCase() ?: false }
                // Avoid LaunchedEffect/DisposableEffect/etc that can use the VM as a key
                .filterNot { callExpression -> callExpression.isRestartableEffect }
                // Avoid explicitly allowlisted Composable names
                .runIfNotNull(allowedForwardingTargetNames) { regex ->
                    filterNot { callExpression ->
                        callExpression.calleeExpression?.text?.matches(regex) == true
                    }
                }
                .flatMap { callExpression ->
                    checkedCallExpressions.add(callExpression)
                    // Get VALUE_ARGUMENT that has a REFERENCE_EXPRESSION. This would map to `viewModel` in this example:
                    // MyComposable(viewModel, ...)
                    callExpression.valueArguments
                        .mapNotNull { valueArgument ->
                            when (val argumentExpression = valueArgument.getArgumentExpression()) {
                                is KtReferenceExpression, is KtThisExpression -> argumentExpression
                                else -> null
                            }
                        }
                        .filter { argumentExpression ->
                            val isItRefAndScopedInVMParams = usesItObjectRef &&
                                argumentExpression.text == "it" && scopedParameter in viewModelParameterNames
                            val isThisRefAndScopedInVMParams = !usesItObjectRef &&
                                argumentExpression.text == "this" && scopedParameter in viewModelParameterNames

                            argumentExpression.text in viewModelParameterNames || isItRefAndScopedInVMParams ||
                                isThisRefAndScopedInVMParams
                        }
                        .map { callExpression }
                }
                .forEach { callExpression ->
                    emitter.report(callExpression, AvoidViewModelForwarding, false)
                }
        }

        val callExpressions = bodyBlock
            .findChildrenByClass<KtCallExpression>()
            .filterNot { it in checkedCallExpressions }
        checkCallExpressions(callExpressions = callExpressions)
    }

    private val KtCallExpression.isScopeFunction: Boolean
        get() = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() in scopeFunctions

    private val KtCallExpression.isWithScope: Boolean
        get() = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "with"

    private val KtCallExpression.hasItObjectReference: Boolean
        get() = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() in itObjectScopeFunctions

    private fun KtCallExpression.getScopedParameterValue(): String? {
        return if (isWithScope) {
            valueArguments.firstOrNull()?.getArgumentExpression()?.text
        } else {
            (parent as? KtDotQualifiedExpression)?.receiverExpression?.text
        }
    }

    companion object {
        private val defaultStateHolderNames = listOf(".*ViewModel", ".*Presenter")
        private val scopeFunctions = setOf("with", "apply", "run", "also", "let")
        private val itObjectScopeFunctions = setOf("let", "also")
        val AvoidViewModelForwarding = """
            Forwarding a ViewModel/Presenter through multiple @Composable functions should be avoided. Consider using state hoisting.

            See https://mrmans0n.github.io/compose-rules/rules/#hoist-all-the-things for more information.
        """.trimIndent()
    }
}
