// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import java.net.URI
import java.util.ArrayDeque

class StaleRememberUpdatedStateInRememberCheck(config: Config) :
    Rule(
        config = config,
        description = "rememberUpdatedState values should not be read eagerly in remember initializers",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#stale-rememberupdatedstate-in-remember"),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("StaleRememberUpdatedStateInRemember")

    private val propertyScopes = ArrayDeque<MutableList<RememberUpdatedStateProperty>>()
    private var memoizingCalculationDepth = 0

    override fun visitNamedFunction(function: KtNamedFunction) {
        withPropertyScope {
            super.visitNamedFunction(function)
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        withPropertyScope {
            super.visitPropertyAccessor(accessor)
        }
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        withPropertyScope {
            super.visitLambdaExpression(lambdaExpression)
        }
    }

    override fun visitProperty(property: KtProperty) {
        if (memoizingCalculationDepth == 0) {
            property.toRememberUpdatedStateProperty()?.let { propertyScopes.peekLast()?.add(it) }
        }
        super.visitProperty(property)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        if (expression.isInsideComposableScope() && expression.isMemoizingComposableCall()) {
            reportStaleReads(expression)
            memoizingCalculationDepth++
            super.visitCallExpression(expression)
            memoizingCalculationDepth--
        } else {
            super.visitCallExpression(expression)
        }
    }

    private fun withPropertyScope(block: () -> Unit) {
        propertyScopes.addLast(mutableListOf())
        try {
            block()
        } finally {
            propertyScopes.removeLast()
        }
    }

    private fun KtProperty.toRememberUpdatedStateProperty(): RememberUpdatedStateProperty? {
        if (!isInsideComposableScope()) return null
        val readKind = if (hasDelegate()) {
            RememberUpdatedStateReadKind.DelegatedValue
        } else {
            RememberUpdatedStateReadKind.StateValue
        }
        val rememberUpdatedStateCall = (delegateExpression ?: initializer)?.rememberUpdatedStateCall() ?: return null
        if (!rememberUpdatedStateCall.isRememberUpdatedStateCall()) return null
        val name = name ?: return null

        return RememberUpdatedStateProperty(
            property = this,
            name = name,
            readKind = readKind,
            sourceExpressions = rememberUpdatedStateCall.argumentExpressions(),
        )
    }

    private fun reportStaleReads(expression: KtCallExpression) {
        val keyExpressions = expression.keyExpressions()
        val properties = propertyScopes
            .flatMap { it }
            .filterNot { property -> property.isKeyedBy(keyExpressions) }
        if (properties.isEmpty()) return

        val propertiesByName = properties.groupBy { it.name }
        val calculation = expression.calculationLambda() ?: return
        calculation.bodyExpression?.accept(
            object : KtTreeVisitorVoid() {
                override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                    if (lambdaExpression == calculation || lambdaExpression.isEagerScopeFunctionLambda()) {
                        super.visitLambdaExpression(lambdaExpression)
                    }
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    // Local functions defer reads until invocation.
                }

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                    // Property getters defer reads until invocation.
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    if (expression.isPropertyDeclarationName()) return
                    if (propertiesByName[expression.getReferencedName()]
                            .orEmpty()
                            .any { property -> property.isStaleRead(expression) }
                    ) {
                        report(
                            Finding(
                                entity = Entity.from(expression),
                                message = StaleRememberUpdatedStateInRemember,
                            ),
                        )
                    }
                    super.visitSimpleNameExpression(expression)
                }
            },
        )
    }

    private fun KtCallExpression.calculationLambda(): KtLambdaExpression? = valueArguments
        .firstOrNull { argument -> argument.argumentName().isInitializerArgumentName() }
        ?.getArgumentExpression() as? KtLambdaExpression
        ?: lambdaArguments.lastOrNull()?.getLambdaExpression()
        ?: valueArguments.lastOrNull()?.getArgumentExpression() as? KtLambdaExpression

    private fun RememberUpdatedStateProperty.isKeyedBy(keyExpressions: List<KtExpression>): Boolean =
        keyExpressions.any { keyExpression ->
            when (readKind) {
                RememberUpdatedStateReadKind.DelegatedValue ->
                    (keyExpression as? KtSimpleNameExpression)?.isResolvedReadOf(property) == true

                RememberUpdatedStateReadKind.StateValue -> keyExpression.isStateValueReadOf(property)
            } ||
                sourceExpressions.any { sourceExpression -> keyExpression.isSameResolvedValueAs(sourceExpression) }
        }

    private fun RememberUpdatedStateProperty.isStaleRead(expression: KtSimpleNameExpression): Boolean =
        expression.isResolvedReadOf(property) &&
            when (readKind) {
                RememberUpdatedStateReadKind.DelegatedValue -> true
                RememberUpdatedStateReadKind.StateValue -> expression.isStateValueReceiver()
            }

    private fun KtCallExpression.keyExpressions(): List<KtExpression> =
        if (isResolvedCallToAnyOf(setOf(ComposeFqNames.RememberSaveable))) {
            rememberSaveableKeyExpressions()
        } else {
            rememberOrRetainKeyExpressions()
        }

    private fun KtCallExpression.rememberOrRetainKeyExpressions(): List<KtExpression> =
        valueArguments.flatMap { argument ->
            val expression = argument.getArgumentExpression() ?: return@flatMap emptyList()
            if (expression is KtLambdaExpression || argument.argumentName() == "calculation") {
                emptyList()
            } else {
                argument.flattenedKeyExpressions()
            }
        }

    private fun KtCallExpression.rememberSaveableKeyExpressions(): List<KtExpression> =
        valueArguments.flatMap { argument ->
            val expression = argument.getArgumentExpression() ?: return@flatMap emptyList()
            if (expression is KtLambdaExpression) return@flatMap emptyList()

            when (argument.argumentName()) {
                null, "inputs" -> argument.flattenedKeyExpressions()
                else -> emptyList()
            }
        }

    private fun KtValueArgument.flattenedKeyExpressions(): List<KtExpression> {
        val expression = getArgumentExpression() ?: return emptyList()
        return expression.flattenKeyExpression(
            containingCall = containingCallExpression(),
            shouldFlattenArrayInitializer = getSpreadElement() != null || argumentName().isArrayContainerArgumentName(),
        )
    }

    private fun KtValueArgument.containingCallExpression(): KtCallExpression? = generateSequence(parent) { element ->
        element.parent
    }.filterIsInstance<KtCallExpression>().firstOrNull()

    private fun KtExpression.flattenKeyExpression(
        containingCall: KtCallExpression? = null,
        shouldFlattenArrayInitializer: Boolean = false,
    ): List<KtExpression> = when {
        this is KtCallExpression && isArrayOfCall() -> argumentExpressions()

        shouldFlattenArrayInitializer && this is KtSimpleNameExpression -> resolvedPropertyInitializer()
            ?.takeUnless { containingCall != null && isMutatedBefore(containingCall) }
            ?.flattenKeyExpression(containingCall = containingCall, shouldFlattenArrayInitializer = true)
            ?: listOf(this)

        else -> listOf(this)
    }

    private fun KtSimpleNameExpression.isMutatedBefore(containingCall: KtCallExpression): Boolean {
        val property = resolvedLocalImmutableProperty() ?: return true
        val scanRoot = property.parent ?: return true
        var isMutated = false
        scanRoot.accept(
            object : KtTreeVisitorVoid() {
                override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                    if (lambdaExpression.isEagerScopeFunctionLambda()) {
                        super.visitLambdaExpression(lambdaExpression)
                    }
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    // Local functions defer writes until invocation.
                }

                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.textOffset >= containingCall.textOffset) return
                    if (expression.isSetCallOn(property)) {
                        isMutated = true
                        return
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (expression.textOffset >= containingCall.textOffset) return
                    if (expression.operationToken == KtTokens.EQ &&
                        (expression.left as? KtArrayAccessExpression)
                            ?.arrayExpression
                            ?.let { arrayExpression -> arrayExpression as? KtSimpleNameExpression }
                            ?.isReadOfArrayAlias(property) == true
                    ) {
                        isMutated = true
                        return
                    }
                    super.visitBinaryExpression(expression)
                }

                override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                    if (expression.textOffset >= containingCall.textOffset) return
                    if ((expression.receiverExpression as? KtSimpleNameExpression)?.isReadOfArrayAlias(
                            property,
                        ) == true &&
                        (expression.selectorExpression as? KtCallExpression)?.calleeExpression?.text == "set"
                    ) {
                        isMutated = true
                        return
                    }
                    super.visitDotQualifiedExpression(expression)
                }
            },
        )

        return isMutated
    }

    private fun KtCallExpression.isSetCallOn(property: KtProperty): Boolean = calleeExpression?.text == "set" &&
        (parent as? KtDotQualifiedExpression)
            ?.receiverExpression
            ?.let { receiver -> receiver as? KtSimpleNameExpression }
            ?.isReadOfArrayAlias(property) == true

    private fun KtSimpleNameExpression.isReadOfArrayAlias(property: KtProperty): Boolean = isResolvedReadOf(property)

    private fun KtCallExpression.isArrayOfCall(): Boolean = calleeExpression?.text == "arrayOf"

    private fun KtCallExpression.argumentExpressions(): List<KtExpression> = valueArguments
        .mapNotNull { argument -> argument.getArgumentExpression() }

    private fun KtExpression.rememberUpdatedStateCall(): KtCallExpression? = when (this) {
        is KtCallExpression -> this
        is KtDotQualifiedExpression -> selectorExpression as? KtCallExpression
        else -> null
    }

    private fun KtValueArgument.argumentName(): String? = getArgumentName()?.asName?.asString()

    private fun String?.isInitializerArgumentName(): Boolean = this == "calculation" || this == "init"

    private fun String?.isArrayContainerArgumentName(): Boolean = this == "keys" || this == "inputs"

    private fun KtSimpleNameExpression.isPropertyDeclarationName(): Boolean =
        (parent as? KtProperty)?.nameIdentifier == this

    private fun KtSimpleNameExpression.isStateValueReceiver(): Boolean =
        (parent as? KtDotQualifiedExpression)?.isStateValueReadOf(this) == true

    private fun KtExpression.isStateValueReadOf(property: KtProperty): Boolean =
        (this as? KtDotQualifiedExpression)?.let { qualifiedExpression ->
            (qualifiedExpression.receiverExpression as? KtSimpleNameExpression)?.isResolvedReadOf(property) == true &&
                qualifiedExpression.selectorExpression?.text == "value"
        } == true

    private fun KtDotQualifiedExpression.isStateValueReadOf(receiver: KtSimpleNameExpression): Boolean =
        receiverExpression == receiver && selectorExpression?.text == "value"

    internal companion object {
        val StaleRememberUpdatedStateInRemember = """
            Reading a `rememberUpdatedState` value directly inside `remember { }`, `rememberSaveable { }`, or `retain { }` captures the initial value only.
            Key the call on the source value or defer the read in a lambda.

            See https://mrmans0n.github.io/compose-rules/rules/#stale-rememberupdatedstate-in-remember for more information.
        """.trimIndent()
    }
}

private data class RememberUpdatedStateProperty(
    val property: KtProperty,
    val name: String,
    val readKind: RememberUpdatedStateReadKind,
    val sourceExpressions: List<KtExpression>,
)

private enum class RememberUpdatedStateReadKind {
    DelegatedValue,
    StateValue,
}
