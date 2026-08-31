// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(KaExperimentalApi::class)

package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expectedType
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.functionType
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaForLoopCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleCall
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
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
        if (!expression.hasKeyedSideEffect()) return

        val body = expression.lambdaArgumentMappedTo("block")?.bodyExpression ?: return
        val effectCallName = expression.calleeExpression?.text
        val requiresCoroutine = body
            .collectDescendantsOfType<KtElement> { candidate -> candidate.canRequireLaunchedEffect() }
            .filterNot { candidate ->
                candidate.canResolveToFunctionCall() &&
                    candidate.parents.takeWhile { parent ->
                        parent != body
                    }.any { parent -> parent is KtNamedFunction } &&
                    !candidate.hasCoroutineScopeReceiver(body) &&
                    !candidate.hasConfiguredCall()
            }
            .filterNot { candidate ->
                candidate.canResolveToFunctionCall() &&
                    candidate.isInsideDeferredLambda(body) &&
                    !candidate.hasCoroutineScopeReceiver(body)
            }
            .any { candidate ->
                if (candidate.canResolveToFunctionCall()) {
                    candidate.isSuspendOrUnresolvedCall(body)
                } else {
                    (candidate as? KtExpression)?.usesCoroutineScope(body, effectCallName) == true
                }
            }

        if (!requiresCoroutine) {
            report(
                Finding(
                    entity = Entity.from(expression.calleeExpression ?: expression),
                    message = UnnecessaryLaunchedEffect,
                ),
            )
        }
    }

    private fun KtElement.canResolveToFunctionCall(): Boolean = when (this) {
        is KtBinaryExpression -> operationToken !in NonFunctionCallBinaryOperations

        is KtCallExpression,
        is KtUnaryExpression,
        is KtArrayAccessExpression,
        is KtDestructuringDeclarationEntry,
        is KtForExpression,
        is KtPropertyDelegate,
        -> true

        else -> false
    }

    private fun KtCallExpression.hasKeyedSideEffect(): Boolean = runCatching {
        analyze(this) {
            findTopLevelCallables(ComposeFqNames.runtime, Name.identifier("SideEffect"))
                .filterIsInstance<KaNamedFunctionSymbol>()
                .any { function ->
                    function.valueParameters.any { parameter -> parameter.name.asString().startsWith("key") }
                }
        }
    }.getOrDefault(false)

    private fun KtElement.canRequireLaunchedEffect(): Boolean =
        canResolveToFunctionCall() || this is KtNameReferenceExpression || this is KtThisExpression

    private fun KtElement.isSuspendOrUnresolvedCall(effectBody: KtExpression): Boolean = runCatching {
        analyze(this) {
            val call = this@isSuspendOrUnresolvedCall.resolveToCall()
                ?.successfulCallOrNull<KaCall>()
                ?: return@analyze true
            when (call) {
                is KaForLoopCall -> call.needsLaunchedEffect()

                is KaFunctionCall<*> -> call.needsLaunchedEffect(
                    hasExplicitReceiver = hasExplicitReceiver(),
                    hasNestedExternalReceiver = isInsideNestedExternalReceiverLambda(effectBody),
                )

                else -> true
            }
        }
    }.getOrDefault(true)

    private fun KtElement.hasCoroutineScopeReceiver(effectBody: KtExpression): Boolean {
        if (hasExplicitReceiver() || isInsideNestedExternalReceiverLambda(effectBody)) return false
        return runCatching {
            analyze(this) {
                when (val call = this@hasCoroutineScopeReceiver.resolveToCall()?.successfulCallOrNull<KaCall>()) {
                    is KaForLoopCall -> listOf(call.iteratorCall, call.hasNextCall, call.nextCall)
                        .any { functionCall -> functionCall.hasCoroutineScopeReceiver() }

                    is KaFunctionCall<*> -> call.hasCoroutineScopeReceiver()

                    else -> false
                }
            }
        }.getOrDefault(false)
    }

    private fun KtExpression.usesCoroutineScope(effectBody: KtExpression, effectCallName: String?): Boolean =
        runCatching {
            analyze(this) {
                if (this@usesCoroutineScope is KtThisExpression &&
                    this@usesCoroutineScope.referencesEffectReceiver(effectCallName) &&
                    (
                        this@usesCoroutineScope.getLabelName() != null ||
                            !isInsideNestedExternalReceiverLambda(effectBody)
                        ) &&
                    this@usesCoroutineScope.expressionType?.symbol?.classId?.asSingleFqName() ==
                    KotlinFqNames.CoroutineScope
                ) {
                    return@analyze true
                }
                val variableAccess = this@usesCoroutineScope.resolveToCall()
                    ?.successfulCallOrNull<KaVariableAccessCall>()
                if (!hasExplicitReceiver() &&
                    !isInsideNestedExternalReceiverLambda(effectBody) &&
                    variableAccess?.hasCoroutineScopeReceiver() == true
                ) {
                    return@analyze true
                }
                val property = (this@usesCoroutineScope as? KtNameReferenceExpression)
                    ?.mainReference
                    ?.resolveToSymbol() as? KaPropertySymbol
                if (!hasExplicitReceiver() &&
                    !isInsideNestedExternalReceiverLambda(effectBody) &&
                    property?.hasCoroutineScopeReceiver() == true
                ) {
                    return@analyze true
                }
                val callableReference = this@usesCoroutineScope.parent as? KtCallableReferenceExpression
                if (!hasExplicitReceiver() &&
                    !isInsideNestedExternalReceiverLambda(effectBody) &&
                    callableReference?.callableReference == this@usesCoroutineScope &&
                    callableReference.receiverExpression == null &&
                    callableReference.resolveToCall()
                        ?.successfulCallOrNull<KaFunctionCall<*>>()
                        ?.hasCoroutineScopeReceiver() == true
                ) {
                    return@analyze true
                }
                val function = (this@usesCoroutineScope as? KtNameReferenceExpression)
                    ?.mainReference
                    ?.resolveToSymbol() as? KaNamedFunctionSymbol
                    ?: return@analyze false
                !hasExplicitReceiver() && !isInsideNestedExternalReceiverLambda(effectBody) &&
                    function.hasCoroutineScopeReceiver()
            }
        }.getOrDefault(false)

    private fun KtThisExpression.referencesEffectReceiver(effectCallName: String?): Boolean {
        val label = getLabelName() ?: return true
        return label == effectCallName ||
            parents.filterIsInstance<KtLabeledExpression>().any { expression -> expression.getLabelName() == label }
    }

    private fun KtElement.hasExplicitReceiver(): Boolean = when (this) {
        is KtBinaryExpression -> left != null

        is KtUnaryExpression -> baseExpression != null

        is KtArrayAccessExpression -> arrayExpression != null

        else ->
            (parent as? KtQualifiedExpression)?.let { qualified ->
                qualified.selectorExpression == this ||
                    qualified.selectorExpression == (this as? KtCallExpression)?.calleeExpression
            } == true ||
                (parent as? KtCallExpression)?.let { call ->
                    call.calleeExpression == this && (call.parent as? KtQualifiedExpression)?.selectorExpression == call
                } == true ||
                (parent as? KtCallableReferenceExpression)?.let { reference ->
                    reference.callableReference == this && reference.receiverExpression != null
                } == true
    }

    private fun KtElement.isInsideDeferredLambda(effectBody: KtExpression): Boolean = parents
        .takeWhile { parent -> parent != effectBody }
        .filterIsInstance<KtLambdaExpression>()
        .any { lambda ->
            lambda.getStrictParentOfType<KtCallExpression>()?.isResolvedInlineArgument(lambda) != true
        }

    private fun KtElement.isInsideNestedExternalReceiverLambda(effectBody: KtExpression): Boolean = parents
        .takeWhile { parent -> parent != effectBody }
        .filterIsInstance<KtLambdaExpression>()
        .any { lambda -> lambda.hasCoroutineScopeReceiver() }

    private fun KaForLoopCall.needsLaunchedEffect(): Boolean =
        listOf(iteratorCall, hasNextCall, nextCall).any { call -> call.needsLaunchedEffect() }

    private fun KaFunctionCall<*>.needsLaunchedEffect(
        hasExplicitReceiver: Boolean = false,
        hasNestedExternalReceiver: Boolean = false,
    ): Boolean {
        val function = symbol as? KaNamedFunctionSymbol
        if (function?.isSuspend == true) return true
        if (isConfiguredCall()) return true
        return (!hasExplicitReceiver && !hasNestedExternalReceiver && hasCoroutineScopeReceiver()) ||
            hasConfiguredReceiverType()
    }

    private fun KaSingleCall<*, *>.hasCoroutineScopeReceiver(): Boolean = receiverTypes()
        .any { type -> type.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope }

    private fun KtElement.hasConfiguredCall(): Boolean = runCatching {
        analyze(this) {
            val call = this@hasConfiguredCall.resolveToCall()
                ?.successfulCallOrNull<KaFunctionCall<*>>()
                ?: return@analyze false
            call.isConfiguredCall() || call.hasConfiguredReceiverType()
        }
    }.getOrDefault(false)

    private fun KtLambdaExpression.hasCoroutineScopeReceiver(): Boolean = runCatching {
        analyze(this) {
            listOfNotNull(
                (functionLiteral.functionType as? KaFunctionType)?.receiverType,
                (expectedType as? KaFunctionType)?.receiverType,
            ).any { type -> type.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope }
        }
    }.getOrDefault(false)

    private fun KaFunctionCall<*>.isConfiguredCall(): Boolean =
        (symbol as? KaNamedFunctionSymbol)?.callableId?.asSingleFqName()?.asString() in allowedCallNamesSet

    private fun KaSingleCall<*, *>.hasConfiguredReceiverType(): Boolean = receiverTypes()
        .mapNotNull { type -> type.symbol?.classId?.asSingleFqName()?.asString() }
        .any { type -> type in allowedCallReceiverTypesSet }

    private fun KaSingleCall<*, *>.receiverTypes() = listOfNotNull(dispatchReceiver?.type, extensionReceiver?.type) +
        contextArguments.map { receiver -> receiver.type }

    private fun KaPropertySymbol.hasCoroutineScopeReceiver(): Boolean =
        receiverParameter?.returnType?.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope ||
            callableId?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope

    private fun KaNamedFunctionSymbol.hasCoroutineScopeReceiver(): Boolean =
        receiverParameter?.returnType?.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope ||
            callableId?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope

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
