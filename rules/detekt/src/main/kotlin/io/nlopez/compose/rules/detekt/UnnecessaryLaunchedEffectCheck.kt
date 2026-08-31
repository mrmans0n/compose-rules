// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import java.net.URI

class UnnecessaryLaunchedEffectCheck(config: Config) :
    Rule(
        config,
        "LaunchedEffect should only be used for work that requires a coroutine.",
        URI("https://mrmans0n.github.io/compose-rules/rules/#avoid-unnecessary-launchedeffect"),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("UnnecessaryLaunchedEffect")

    private val allowedCallReceiverTypes by config(defaultValue = emptyList<String>())
    private val allowedCallReceiverTypesSet by lazy { allowedCallReceiverTypes.toSet() }
    private val allowedCallNames by config(defaultValue = emptyList<String>())
    private val allowedCallNamesSet by lazy { allowedCallNames.toSet() }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.isResolvedCallToAnyOf(setOf(ComposeFqNames.LaunchedEffect))) return

        val body = expression.lambdaArgumentMappedTo("block")?.bodyExpression ?: return
        val requiresCoroutine = body
            .collectDescendantsOfType<KtExpression> { candidate -> candidate.canResolveToFunctionCall() }
            .filterNot { candidate ->
                candidate.parents.takeWhile { parent -> parent != body }.any { parent -> parent is KtNamedFunction }
            }
            .filterNot { candidate -> candidate.isInsideDeferredLambda(body) }
            .any { candidate -> candidate.isSuspendOrUnresolvedCall() }

        if (!requiresCoroutine) {
            report(
                Finding(
                    entity = Entity.from(expression.calleeExpression ?: expression),
                    message = UnnecessaryLaunchedEffect,
                ),
            )
        }
    }

    private fun KtExpression.canResolveToFunctionCall(): Boolean = when (this) {
        is KtBinaryExpression -> operationToken !in NonFunctionCallBinaryOperations
        is KtCallExpression, is KtUnaryExpression, is KtArrayAccessExpression -> true
        else -> false
    }

    private fun KtExpression.isSuspendOrUnresolvedCall(): Boolean = runCatching {
        analyze(this) {
            val call = this@isSuspendOrUnresolvedCall.resolveToCall()
                ?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
                ?: return@analyze true
            when (call) {
                is KaFunctionCall<*> -> call.needsLaunchedEffect()
                else -> true
            }
        }
    }.getOrDefault(true)

    private fun KtExpression.isInsideDeferredLambda(effectBody: KtExpression): Boolean = parents
        .takeWhile { parent -> parent != effectBody }
        .filterIsInstance<KtLambdaExpression>()
        .any { lambda ->
            lambda.getStrictParentOfType<KtCallExpression>()?.isResolvedInlineArgument(lambda) != true
        }

    private fun KaFunctionCall<*>.needsLaunchedEffect(): Boolean {
        val function = symbol as? KaNamedFunctionSymbol
        if (function?.isSuspend == true) return true
        val callableName = function?.callableId?.asSingleFqName()?.asString()
        if (callableName != null && callableName in allowedCallNamesSet) return true
        val receiverTypes = listOfNotNull(dispatchReceiver?.type, extensionReceiver?.type)
            .mapNotNull { type -> type.symbol?.classId?.asSingleFqName()?.asString() }
        return KotlinFqNames.CoroutineScope.asString() in receiverTypes ||
            receiverTypes.any { type -> type in allowedCallReceiverTypesSet }
    }

    internal companion object {
        private val NonFunctionCallBinaryOperations = setOf(
            KtTokens.EQ,
            KtTokens.ANDAND,
            KtTokens.OROR,
            KtTokens.ELVIS,
            KtTokens.EQEQEQ,
            KtTokens.EXCLEQEQEQ,
        )

        val UnnecessaryLaunchedEffect = """
            LaunchedEffect is unnecessary when its body only calls non-suspending functions. Use keyed SideEffect instead.
        """.trimIndent()
    }
}
