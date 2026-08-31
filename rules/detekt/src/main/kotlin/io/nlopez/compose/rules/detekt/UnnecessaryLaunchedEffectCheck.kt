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
import org.jetbrains.kotlin.analysis.api.components.allSupertypes
import org.jetbrains.kotlin.analysis.api.components.expectedType
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.functionType
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.components.type
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaDelegatedPropertyCall
import org.jetbrains.kotlin.analysis.api.resolution.KaForLoopCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleCall
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
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
                candidate is KtCallableReferenceExpression && !candidate.isInvokedCallableReference(body)
            }
            .filterNot { candidate ->
                candidate.canResolveToFunctionCall() &&
                    candidate.isInsideUnusedLocalFunction(body) &&
                    !candidate.hasCoroutineScopeReceiver(body)
            }
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
                    !candidate.isInsideInvokedLocalLambda(body) &&
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
        is KtCallableReferenceExpression,
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
                is KaDelegatedPropertyCall -> call.needsLaunchedEffect(hasExplicitReceiver = hasExplicitReceiver())

                is KaForLoopCall -> call.needsLaunchedEffect(hasExplicitReceiver = hasExplicitReceiver())

                is KaFunctionCall<*> -> call.needsLaunchedEffect(
                    hasExplicitReceiver = hasExplicitReceiver(),
                    hasNestedExternalReceiver = isInsideNestedExternalReceiverLambda(effectBody),
                )

                else -> true
            }
        }
    }.getOrDefault(true)

    private fun KtElement.hasCoroutineScopeReceiver(effectBody: KtExpression): Boolean = runCatching {
        analyze(this) {
            val includeFunctionReceivers =
                !hasExplicitReceiver() && !isInsideNestedExternalReceiverLambda(effectBody)
            when (val call = this@hasCoroutineScopeReceiver.resolveToCall()?.successfulCallOrNull<KaCall>()) {
                is KaForLoopCall -> listOf(call.iteratorCall, call.hasNextCall, call.nextCall)
                    .any { functionCall ->
                        functionCall.hasCoroutineScopeReceiver(includeFunctionReceivers = includeFunctionReceivers)
                    }

                is KaFunctionCall<*> -> call.hasCoroutineScopeReceiver(
                    includeFunctionReceivers = includeFunctionReceivers,
                )

                else -> false
            }
        }
    }.getOrDefault(false)

    private fun KtExpression.usesCoroutineScope(effectBody: KtExpression, effectCallName: String?): Boolean =
        runCatching {
            analyze(this) {
                if (this@usesCoroutineScope is KtThisExpression &&
                    this@usesCoroutineScope.referencesEffectReceiver(effectCallName, effectBody) &&
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
                if (!isInsideNestedExternalReceiverLambda(effectBody) &&
                    variableAccess?.hasCoroutineScopeReceiver(
                        includeFunctionReceivers = !hasExplicitReceiver(),
                    ) == true
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

    private fun KtThisExpression.referencesEffectReceiver(effectCallName: String?, effectBody: KtExpression): Boolean {
        val label = getLabelName() ?: return true
        val effectLambda = effectBody.getStrictParentOfType<KtLambdaExpression>()
        val labeledExpression = parents
            .filterIsInstance<KtLabeledExpression>()
            .firstOrNull { expression -> expression.getLabelName() == label }
        if (labeledExpression != null) {
            return labeledExpression.baseExpression?.unwrapArgumentExpression() == effectLambda
        }
        val callSiteLabel = parents
            .filterIsInstance<KtLambdaExpression>()
            .firstOrNull { lambda ->
                lambda.getStrictParentOfType<KtCallExpression>()?.calleeExpression?.text == label
            }
        if (callSiteLabel != null) return callSiteLabel == effectLambda
        return label == effectCallName
    }

    private fun KtElement.hasExplicitReceiver(): Boolean = when (this) {
        is KtBinaryExpression -> left != null

        is KtUnaryExpression -> baseExpression != null

        is KtArrayAccessExpression -> arrayExpression != null

        is KtForExpression -> loopRange != null

        is KtDestructuringDeclarationEntry -> (parent as? KtDestructuringDeclaration)?.initializer != null

        is KtPropertyDelegate -> expression != null

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
        .any { parent ->
            when (parent) {
                is KtLambdaExpression -> parent.hasCoroutineScopeReceiver()
                is KtNamedFunction -> parent.hasCoroutineScopeReceiver()
                else -> false
            }
        }

    private fun KtElement.isInsideUnusedLocalFunction(effectBody: KtExpression): Boolean {
        val localFunction = parents
            .takeWhile { parent -> parent != effectBody }
            .filterIsInstance<KtNamedFunction>()
            .firstOrNull()
            ?: return false
        if (localFunction.isInlineArgument()) return false
        if (localFunction.isInvokedFunctionValue(effectBody)) return false
        return !localFunction.isCalledFrom(effectBody)
    }

    private fun KtNamedFunction.isCalledFrom(effectBody: KtExpression): Boolean {
        val localFunctions = effectBody.collectDescendantsOfType<KtNamedFunction>().toSet()
        val reachableFunctions = mutableSetOf<KtNamedFunction>()
        do {
            val previousSize = reachableFunctions.size
            (listOf(effectBody) + reachableFunctions.mapNotNull { function -> function.bodyExpression })
                .flatMap { scope -> scope.calledLocalFunctions(localFunctions) }
                .forEach { function -> reachableFunctions.add(function) }
        } while (reachableFunctions.size != previousSize)
        return this in reachableFunctions
    }

    private fun KtElement.calledLocalFunctions(localFunctions: Set<KtNamedFunction>): List<KtNamedFunction> =
        collectDescendantsOfType<KtCallExpression> { call -> call.isReachableInCurrentFunctionBody(this) }
            .mapNotNull { call -> call.resolvedLocalFunction(localFunctions) } +
            collectDescendantsOfType<KtCallableReferenceExpression> { reference ->
                reference.isReachableInCurrentFunctionBody(this) && reference.isInvokedIn(this)
            }.mapNotNull { reference -> reference.resolvedLocalFunction(localFunctions) } +
            calledLocalFunctionReferences(localFunctions)

    private fun KtElement.calledLocalFunctionReferences(localFunctions: Set<KtNamedFunction>): List<KtNamedFunction> {
        val references = collectDescendantsOfType<KtProperty> { property -> property.isInCurrentFunctionBody(this) }
            .mapNotNull { property ->
                val name = property.name ?: return@mapNotNull null
                val reference = property.initializer?.unwrapArgumentExpression() as? KtCallableReferenceExpression
                    ?: return@mapNotNull null
                name to reference.resolvedLocalFunction(localFunctions)
            }.toMap()
        if (references.isEmpty()) return emptyList()
        return collectDescendantsOfType<KtCallExpression> { call -> call.isInCurrentFunctionBody(this) }
            .mapNotNull { call ->
                references[call.referencedLocalFunctionValueName()]
            }
    }

    private fun KtCallExpression.referencedLocalFunctionValueName(): String? =
        ((parent as? KtQualifiedExpression)?.receiverExpression as? KtNameReferenceExpression)
            ?.takeIf {
                (parent as? KtQualifiedExpression)?.selectorExpression == this &&
                    (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "invoke"
            }
            ?.getReferencedName()
            ?: (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

    private fun KtElement.isInCurrentFunctionBody(scope: KtElement): Boolean =
        parents.takeWhile { parent -> parent != scope }.none { parent -> parent is KtNamedFunction } &&
            (scope !is KtExpression || !isInsideDeferredLambda(scope))

    private fun KtElement.isReachableInCurrentFunctionBody(scope: KtElement): Boolean =
        isInCurrentFunctionBody(scope) || (scope is KtExpression && isInsideDirectlyInvokedLocalLambda(scope))

    private fun KtElement.isInsideInvokedLocalLambda(effectBody: KtExpression): Boolean =
        nearestDeferredLambda(effectBody)
            ?.let { lambda ->
                lambda.isDirectlyInvoked(effectBody) ||
                    lambda.localLambdaProperty()?.isInvokedIn(effectBody) == true
            } == true

    private fun KtElement.isInsideDirectlyInvokedLocalLambda(scope: KtExpression): Boolean =
        nearestDeferredLambda(scope)
            ?.let { lambda ->
                lambda.isDirectlyInvoked(scope) ||
                    lambda.localLambdaProperty()?.isDirectlyInvokedIn(scope) == true
            } == true

    private fun KtElement.nearestDeferredLambda(scope: KtExpression): KtLambdaExpression? =
        parents.takeWhile { parent -> parent != scope }
            .filterIsInstance<KtLambdaExpression>()
            .firstOrNull { lambda ->
                lambda.getStrictParentOfType<KtCallExpression>()?.isResolvedInlineArgument(lambda) != true
            }

    private fun KtLambdaExpression.localLambdaProperty(): KtProperty? = getStrictParentOfType<KtProperty>()
        ?.takeIf { property -> property.initializer?.unwrapArgumentExpression() == this }

    private fun KtNamedFunction.isInvokedFunctionValue(effectBody: KtExpression): Boolean =
        getStrictParentOfType<KtProperty>()
            ?.takeIf { property -> property.initializer?.unwrapArgumentExpression() == this }
            ?.isInvokedIn(effectBody) == true

    private fun KtNamedFunction.isInlineArgument(): Boolean =
        getStrictParentOfType<KtCallExpression>()?.isResolvedInlineArgument(this) == true

    private fun KtLambdaExpression.isDirectlyInvoked(scope: KtElement): Boolean = parents
        .takeWhile { parent -> parent != scope && parent !is KtProperty }
        .filterIsInstance<KtCallExpression>()
        .any { call ->
            call.calleeExpression == this ||
                parents.takeWhile { parent -> parent != call }.any { parent -> parent == call.calleeExpression }
        }

    private fun KtCallableReferenceExpression.isDirectlyInvoked(scope: KtElement): Boolean = parents
        .takeWhile { parent -> parent != scope && parent !is KtProperty }
        .filterIsInstance<KtCallExpression>()
        .any { call ->
            call.calleeExpression == this ||
                parents.takeWhile { parent -> parent != call }.any { parent -> parent == call.calleeExpression }
        }

    private fun KtCallableReferenceExpression.isInvokedIn(scope: KtElement): Boolean =
        isDirectlyInvoked(scope) || getStrictParentOfType<KtCallExpression>()?.isResolvedInlineArgument(this) == true

    private fun KtCallableReferenceExpression.isInvokedCallableReference(effectBody: KtExpression): Boolean {
        if (isInvokedIn(effectBody)) return true
        val property = getStrictParentOfType<KtProperty>()
            ?.takeIf { property -> property.initializer?.unwrapArgumentExpression() == this }
            ?: return false
        return property.isInvokedIn(effectBody)
    }

    private fun KtProperty.isInvokedIn(effectBody: KtExpression): Boolean =
        effectBody.reachableInvocationScopes().any { scope ->
            scope.hasDirectInvocationOf(scope.localFunctionValueProperties(setOf(this)))
        }

    private fun KtExpression.reachableInvocationScopes(): Set<KtExpression> {
        val scopes = (
            listOf(this) +
                collectDescendantsOfType<KtNamedFunction>()
                    .filter { function -> function.isCalledFrom(this) }
                    .mapNotNull { function -> function.bodyExpression }
            ).toMutableSet()
        do {
            val previousSize = scopes.size
            scopes.toList()
                .flatMap { scope ->
                    scope.collectDescendantsOfType<KtProperty> { property ->
                        property.isInCurrentFunctionBody(scope) && property.isDirectlyInvokedIn(scope)
                    }
                }
                .mapNotNull { property -> property.functionValueBodyExpression() }
                .forEach { body -> scopes.add(body) }
        } while (scopes.size != previousSize)
        return scopes
    }

    private fun KtProperty.functionValueBodyExpression(): KtExpression? =
        when (val initializer = initializer?.unwrapArgumentExpression()) {
            is KtLambdaExpression -> initializer.bodyExpression
            is KtNamedFunction -> initializer.bodyExpression
            else -> null
        }

    private fun KtProperty.isDirectlyInvokedIn(scope: KtExpression): Boolean =
        scope.hasDirectInvocationOf(scope.localFunctionValueProperties(setOf(this)))

    private fun KtExpression.hasDirectInvocationOf(properties: Set<KtProperty>): Boolean {
        val scope = this
        return scope.collectDescendantsOfType<KtCallExpression> { call ->
            call.isInCurrentFunctionBody(scope)
        }.any { call -> call.referencedLocalFunctionValueProperty(scope, properties) in properties } ||
            scope.collectDescendantsOfType<KtNameReferenceExpression> { reference ->
                reference.isInCurrentFunctionBody(scope)
            }.any { reference ->
                reference.referencedLocalProperty(scope, properties) in properties &&
                    reference.getStrictParentOfType<KtCallExpression>()?.isResolvedInlineArgument(reference) == true
            }
    }

    private fun KtExpression.localFunctionValueProperties(seedProperties: Set<KtProperty>): Set<KtProperty> {
        val properties = seedProperties.toMutableSet()
        do {
            val previousSize = properties.size
            collectDescendantsOfType<KtProperty> { property -> property.isInCurrentFunctionBody(this) }
                .filter { property ->
                    (property.initializer?.unwrapArgumentExpression() as? KtNameReferenceExpression)
                        ?.resolvedLocalProperty() in properties
                }
                .forEach { property -> properties.add(property) }
        } while (properties.size != previousSize)
        return properties
    }

    private fun KtCallExpression.referencedLocalFunctionValueProperty(
        scope: KtExpression,
        properties: Set<KtProperty>,
    ): KtProperty? = (
        ((parent as? KtQualifiedExpression)?.receiverExpression as? KtNameReferenceExpression)
            ?.takeIf {
                (parent as? KtQualifiedExpression)?.selectorExpression == this &&
                    (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "invoke"
            }
            ?: calleeExpression as? KtNameReferenceExpression
        )
        ?.referencedLocalProperty(scope, properties)

    private fun KtNameReferenceExpression.referencedLocalProperty(
        scope: KtExpression,
        properties: Set<KtProperty>,
    ): KtProperty? = resolvedLocalProperty()
        ?: scope.visibleLocalProperty(getReferencedName(), this)
        ?: properties.singleOrNull { property -> property.name == getReferencedName() }

    private fun KtExpression.visibleLocalProperty(name: String, reference: KtElement): KtProperty? =
        collectDescendantsOfType<KtProperty> { property ->
            property.isInCurrentFunctionBody(this) &&
                property.name == name &&
                property.textOffset < reference.textOffset
        }.maxByOrNull { property -> property.textOffset }

    private fun KtNameReferenceExpression.resolvedLocalProperty(): KtProperty? = runCatching {
        analyze(this) {
            val variableAccess = this@resolvedLocalProperty.resolveToCall() as? KaVariableAccessCall
            val variableAccessProperty = variableAccess?.signature?.symbol as? KaPropertySymbol
            variableAccessProperty?.sourcePsiSafe<KtProperty>()
                ?: mainReference.resolveToSymbol()?.sourcePsiSafe<KtProperty>()
        }
    }.getOrNull()

    private fun KtCallExpression.resolvedLocalFunction(localFunctions: Set<KtNamedFunction>): KtNamedFunction? =
        runCatching {
            analyze(this) {
                resolveToCall()
                    ?.successfulCallOrNull<KaFunctionCall<*>>()
                    ?.symbol
                    ?.sourcePsiSafe<KtNamedFunction>()
                    ?.takeIf { function -> function in localFunctions }
            }
        }.getOrNull()

    private fun KtCallableReferenceExpression.resolvedLocalFunction(
        localFunctions: Set<KtNamedFunction>,
    ): KtNamedFunction? = runCatching {
        analyze(this) {
            (callableReference.mainReference.resolveToSymbol() as? KaNamedFunctionSymbol)
                ?.sourcePsiSafe<KtNamedFunction>()
                ?.takeIf { function -> function in localFunctions }
                ?: resolveToCall()
                    ?.successfulCallOrNull<KaFunctionCall<*>>()
                    ?.symbol
                    ?.sourcePsiSafe<KtNamedFunction>()
                    ?.takeIf { function -> function in localFunctions }
        }
    }.getOrNull()

    private fun KaDelegatedPropertyCall.needsLaunchedEffect(hasExplicitReceiver: Boolean): Boolean =
        listOfNotNull(provideDelegateCall, valueGetterCall, valueSetterCall)
            .any { call -> call.needsLaunchedEffect(hasExplicitReceiver = hasExplicitReceiver) }

    private fun KaForLoopCall.needsLaunchedEffect(hasExplicitReceiver: Boolean = false): Boolean =
        iteratorCall.needsLaunchedEffect(hasExplicitReceiver = hasExplicitReceiver) ||
            listOf(hasNextCall, nextCall).any { call -> call.needsLaunchedEffect(hasExplicitReceiver = true) }

    private fun KaFunctionCall<*>.needsLaunchedEffect(
        hasExplicitReceiver: Boolean = false,
        hasNestedExternalReceiver: Boolean = false,
    ): Boolean {
        val function = symbol as? KaNamedFunctionSymbol
        if (function?.isSuspend == true) return true
        if (isConfiguredCall()) return true
        val hasEffectCoroutineScopeReceiver =
            !hasNestedExternalReceiver &&
                hasCoroutineScopeReceiver(
                    includeFunctionReceivers = !hasExplicitReceiver,
                )
        return hasEffectCoroutineScopeReceiver || hasConfiguredReceiverType()
    }

    private fun KaSingleCall<*, *>.hasCoroutineScopeReceiver(includeFunctionReceivers: Boolean = true): Boolean =
        receiverTypes(includeFunctionReceivers)
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
            ).any { type ->
                !type.isMarkedNullable &&
                    (
                        type.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope ||
                            type.allSupertypes.any { supertype ->
                                supertype.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope
                            }
                        )
            }
        }
    }.getOrDefault(false)

    private fun KtNamedFunction.hasCoroutineScopeReceiver(): Boolean = runCatching {
        analyze(this) {
            val receiverType = receiverTypeReference?.type ?: return@analyze false
            !receiverType.isMarkedNullable &&
                (
                    receiverType.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope ||
                        receiverType.allSupertypes.any { supertype ->
                            supertype.symbol?.classId?.asSingleFqName() == KotlinFqNames.CoroutineScope
                        }
                    )
        }
    }.getOrDefault(false)

    private fun KaFunctionCall<*>.isConfiguredCall(): Boolean =
        (symbol as? KaNamedFunctionSymbol)?.callableId?.asSingleFqName()?.asString() in allowedCallNamesSet

    private fun KaSingleCall<*, *>.hasConfiguredReceiverType(): Boolean = receiverTypes()
        .mapNotNull { type -> type.symbol?.classId?.asSingleFqName()?.asString() }
        .any { type -> type in allowedCallReceiverTypesSet }

    private fun KaSingleCall<*, *>.receiverTypes(includeFunctionReceivers: Boolean = true) =
        if (includeFunctionReceivers) {
            listOfNotNull(dispatchReceiver?.type, extensionReceiver?.type)
        } else {
            emptyList()
        } + contextArguments.map { receiver -> receiver.type }

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
