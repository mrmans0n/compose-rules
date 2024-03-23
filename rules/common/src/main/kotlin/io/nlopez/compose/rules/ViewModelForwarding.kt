// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.definedInInterface
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isActual
import io.nlopez.rules.core.util.isOverride
import io.nlopez.rules.core.util.isRestartableEffect
import org.jetbrains.kotlin.psi.KtBlockExpression
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

        val stateHolderValidNames = Regex(
            (config.getList("allowedStateHolderNames", emptyList()) + defaultStateHolderNames)
                .joinToString(
                    separator = "|",
                    prefix = "(",
                    postfix = ")",
                ),
        )

        val allowedForwarding = config.getSet("allowedForwarding", emptySet())
        val allowedForwardingRegex = when {
            allowedForwarding.isNotEmpty() -> Regex(
                allowedForwarding.joinToString(
                    separator = "|",
                    prefix = "(",
                    postfix = ")",
                ),
            )

            else -> null
        }

        val viewModelParameterNames = parameters.filter { parameter ->
            // We can't do much better than looking at the types at face value
            parameter.typeReference?.text?.matches(stateHolderValidNames) == true
        }
            .mapNotNull { it.name }
            .toSet()

        val checkedCallExpressions = HashSet<KtCallExpression>()
        fun checkCallExpressions(
            bodyExpression: KtBlockExpression?,
            scopedParameter: String? = null,
            usesItObjectRef: Boolean = false,
        ) {
            bodyExpression?.findChildrenByClass<KtCallExpression>()
                ?.filterNot { it in checkedCallExpressions }
                ?.forEach { callExpression ->
                    checkedCallExpressions.add(callExpression)
                    // We want now to see if these parameter names are used in any other calls to functions that start with
                    // a capital letter (so, most likely, composables).
                    if (callExpression.calleeExpression?.text?.first()?.isUpperCase() == true &&
                        // Avoid LaunchedEffect/DisposableEffect/etc that can use the VM as a key
                        !callExpression.isRestartableEffect &&
                        // Avoid explicitly allowlisted Composable names
                        allowedForwardingRegex?.let { callExpression.calleeExpression?.text?.matches(it) } != true
                    ) {
                        // Get VALUE_ARGUMENT that has a REFERENCE_EXPRESSION. This would map to `viewModel` in this example:
                        // MyComposable(viewModel, ...)
                        callExpression.valueArguments
                            .mapNotNull { valueArgument ->
                                when (val argumentExpression = valueArgument.getArgumentExpression()) {
                                    is KtReferenceExpression, is KtThisExpression -> argumentExpression
                                    else -> null
                                }
                            }
                            .filter { reference ->
                                reference.text in viewModelParameterNames ||
                                    (
                                        reference.text == "it" &&
                                            scopedParameter in viewModelParameterNames && usesItObjectRef
                                        ) ||
                                    (
                                        reference.text == "this" &&
                                            scopedParameter in viewModelParameterNames && !usesItObjectRef
                                        )
                            }
                            .forEach { _ ->
                                emitter.report(callExpression, AvoidViewModelForwarding, false)
                            }
                    }

                    val scopedFunctions = setOf("with", "apply", "run", "also", "let")
                    // Check if the call is a scope function
                    if (callExpression.calleeExpression is KtNameReferenceExpression &&
                        (callExpression.calleeExpression as KtNameReferenceExpression)
                            .getReferencedName() in scopedFunctions
                    ) {
                        callExpression.lambdaArguments
                            .mapNotNull { it.getLambdaExpression()?.bodyExpression }
                            .forEach { lambdaBodyExpression ->
                                checkCallExpressions(
                                    bodyExpression = lambdaBodyExpression,
                                    scopedParameter = callExpression.getScopedParameterValue(scopedParameter),
                                    usesItObjectRef = callExpression.hasItObjectReference,
                                )
                            }
                    }
                }
        }
        checkCallExpressions(bodyBlock)
    }

    private val KtCallExpression.isWithScope: Boolean
        get() = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "with"

    private val KtCallExpression.hasItObjectReference: Boolean
        get() = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() in setOf("let", "also")

    private fun KtCallExpression.getScopedParameterValue(default: String?): String? {
        return when {
            isWithScope -> valueArguments.firstOrNull()?.getArgumentExpression()?.text
            !isWithScope -> (parent as? KtDotQualifiedExpression)?.receiverExpression?.text
            else -> default
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
