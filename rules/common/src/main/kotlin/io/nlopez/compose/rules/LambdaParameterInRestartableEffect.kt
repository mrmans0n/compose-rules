// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.findShadowingRedeclarations
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.isLambda
import io.nlopez.compose.core.util.isRestartableEffect
import io.nlopez.compose.core.util.lambdaTypes
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.isDotSelector

class LambdaParameterInRestartableEffect : ComposeKtVisitor {
    override fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {
        val lambdaTypes = file.lambdaTypes(config)
        val composables = file.findChildrenByClass<KtFunction>()
            .filter { it.isComposable }

        for (composable in composables) {
            // We are only interested in composables with restartable effects (the ones that have keys)
            val effects = composable.findChildrenByClass<KtCallExpression>()
                .filter { it.isRestartableEffect }

            if (effects.none()) continue

            // And of those, we are only interested in composables that have lambda params
            val lambdaParameters = composable.valueParameters
                .filter { it.isLambdaParameter || it.typeReference?.isLambda(lambdaTypes) == true }
                .filter { it.name != null }
                .associateBy { it.name!! }
            val lambdaParameterNames = lambdaParameters.keys

            if (lambdaParameterNames.isEmpty()) continue

            // Then, we just want the lambda parameters that are actually used inside any of the found effects'
            // trailing lambda code
            val usedLambdaParameterNames = effects
                .flatMap { effect ->
                    val body = effect.lambdaArguments.lastOrNull()?.getLambdaExpression()?.bodyExpression
                        ?: return@flatMap emptySequence()

                    val callExpressions = body.findChildrenByClass<KtCallExpression>()
                        // Filter out dot receivers e.g. `something.myLambda()`
                        .filterNot { it.isDotSelector() }

                    val effectCallee = effect.calleeExpression?.text
                    val isDisposableEffect = effectCallee == "DisposableEffect"
                    val isLifecycleEffect = effectCallee == "LifecycleStartEffect" ||
                        effectCallee == "LifecycleResumeEffect"

                    // Lambdas used directly: myLambda()
                    val invoked = callExpressions
                        .let { expressions ->
                            when {
                                isDisposableEffect -> {
                                    expressions.filter { it.calleeExpression?.text != "onDispose" }
                                }
                                isLifecycleEffect -> {
                                    expressions.filter { it.calleeExpression?.text !in LifecycleEffectScopeFunctions }
                                }
                                else -> {
                                    expressions
                                }
                            }
                        }
                        .mapNotNull { it.calleeExpression?.text }
                        .filter { it in lambdaParameterNames }

                    // Lambdas being tossed around to other methods
                    val forwarded = callExpressions.flatMap {
                        it.valueArguments.mapNotNull { argument ->
                            when (val expression = argument.getArgumentExpression()) {
                                // something(myLambda) || something(a = myLambda)
                                is KtReferenceExpression -> {
                                    if (expression.text in lambdaParameterNames) expression.text else null
                                }
                                // something(if (x) myLambda else otherThing)
                                is KtIfExpression -> {
                                    if (expression.then?.text in lambdaParameterNames) {
                                        expression.then?.text
                                    } else if (expression.`else`?.text in lambdaParameterNames) {
                                        expression.`else`?.text
                                    } else {
                                        null
                                    }
                                }

                                else -> null
                            }
                        }
                    }
                    invoked + forwarded
                }
                .toSet()

            // We want to filter out the parameters that are used as key in the restartable effect
            val keyedLambdaParameterNames = effects.flatMap { it.valueArguments }
                .mapNotNull { it.getArgumentExpression() }
                .filterIsInstance<KtReferenceExpression>()
                .map { it.text }
                .filter { it in usedLambdaParameterNames }
                .toSet()

            val shadowedParameters = effects.flatMap { effect ->
                usedLambdaParameterNames.filter {
                    effect.findShadowingRedeclarations(parameterName = it, stopAt = composable).any()
                }
            }.toSet()
            for (parameterName in usedLambdaParameterNames - keyedLambdaParameterNames - shadowedParameters) {
                val parameter = lambdaParameters[parameterName]!!
                emitter.report(parameter, LambdaUsedInRestartableEffect)
            }
        }
    }

    companion object {
        val LambdaUsedInRestartableEffect = """
            Lambda parameters in a @Composable that are referenced directly inside of restarting effects can cause issues or unpredictable behavior.
            If restarting the effect is ok, you can add the reference to this parameter as a key in that effect, so when the parameter changes, a new effect is created.
            However, if the effect is not to be restarted, you will need to use `rememberUpdatedState` on the parameter and use its result in the effect.
            See https://mrmans0n.github.io/compose-rules/rules/#be-mindful-of-the-arguments-you-use-inside-of-a-restarting-effect for more information.
        """.trimIndent()

        private val LifecycleEffectScopeFunctions = setOf("onStopOrDispose", "onPauseOrDispose")
    }
}
