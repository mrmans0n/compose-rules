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
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.net.URI

/**
 * Reports `@Composable` declarations that do not use composition and can remove the annotation.
 */
class UnnecessaryComposableCheck(config: Config) :
    Rule(
        config,
        "Composable declarations that do not use composition should not be marked as @Composable.",
        URI(
            "https://mrmans0n.github.io/compose-rules/rules/#do-not-mark-functions-as-composable-when-they-dont-need-it",
        ),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("UnnecessaryComposable")

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isComposable()) return
        if (function.isReadOnlyComposable()) return
        if (function.isContractDeclaration()) return
        if (function.hasComposableSlotParameter()) return
        val body = function.bodyExpression ?: return
        if ((body as? KtBlockExpression)?.statements?.isEmpty() == true) return
        if (body.usesComposition()) return
        if (function.valueParameters.any { parameter -> parameter.defaultValue?.usesComposition() == true }) return

        reportUnnecessaryComposable(function)
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        super.visitPropertyAccessor(accessor)
        if (!accessor.isGetter) return
        if (!accessor.isComposable()) return
        if (accessor.isReadOnlyComposable()) return
        if (accessor.isContractDeclaration()) return
        val body = accessor.bodyExpression ?: return
        if ((body as? KtBlockExpression)?.statements?.isEmpty() == true) return
        if (body.usesComposition()) return

        reportUnnecessaryComposable(accessor)
    }

    private fun reportUnnecessaryComposable(element: KtElement) {
        report(
            Finding(
                Entity.from(element),
                UnnecessaryComposable,
            ),
        )
    }

    private fun KtElement.usesComposition(): Boolean {
        var usesComposition = false
        accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (usesComposition) return
                    if (expression.isComposableCall()) {
                        usesComposition = true
                        return
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    if (usesComposition) return
                    if (expression.isComposablePropertyRead() || expression.isDelegatedComposeStateRead()) {
                        usesComposition = true
                        return
                    }
                    super.visitSimpleNameExpression(expression)
                }

                override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                    if (usesComposition) return
                    if (expression.isCompositionLocalCurrentRead() || expression.isComposeStateValueReadExpression()) {
                        usesComposition = true
                        return
                    }
                    super.visitDotQualifiedExpression(expression)
                }

                override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression) {
                    if (usesComposition) return
                    if (expression.isComposeStateValueReadExpression()) {
                        usesComposition = true
                        return
                    }
                    super.visitSafeQualifiedExpression(expression)
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    // Nested function bodies are not evaluated by the composable declaration.
                }

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                    // Nested accessor bodies are not evaluated by the composable declaration.
                }

                override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                    if (lambdaExpression.isEagerScopeFunctionLambda()) {
                        super.visitLambdaExpression(lambdaExpression)
                    }
                }
            },
        )
        return usesComposition
    }

    private fun KtNamedFunction.isContractDeclaration(): Boolean {
        if (hasAnyContractModifier()) return true
        return getStrictParentOfType<KtClass>()?.isInterface() == true
    }

    private fun KtPropertyAccessor.isContractDeclaration(): Boolean {
        if (property.hasAnyContractModifier()) return true
        return getStrictParentOfType<KtClass>()?.isInterface() == true
    }

    private fun KtNamedFunction.hasComposableSlotParameter(): Boolean =
        valueParameters.any(KtParameter::hasComposableType) ||
            receiverTypeReference?.hasComposableType() == true

    internal companion object {
        val UnnecessaryComposable = """
            This @Composable declaration does not use composition and should not be marked @Composable.

            See https://mrmans0n.github.io/compose-rules/rules/#do-not-mark-functions-as-composable-when-they-dont-need-it for more information.
        """.trimIndent()
    }
}

private val ContractModifiers = setOf(
    KtTokens.OVERRIDE_KEYWORD,
    KtTokens.OPEN_KEYWORD,
    KtTokens.ABSTRACT_KEYWORD,
    KtTokens.EXPECT_KEYWORD,
    KtTokens.ACTUAL_KEYWORD,
    KtTokens.EXTERNAL_KEYWORD,
)

private fun KtNamedFunction.hasAnyContractModifier(): Boolean = ContractModifiers.any(::hasModifier)

private fun KtProperty.hasAnyContractModifier(): Boolean = ContractModifiers.any(::hasModifier)

private fun KtQualifiedExpression.isComposeStateValueReadExpression(): Boolean =
    isComposeStateValueRead() && !isAssignmentLeftHandSide()

private fun KtElement.isAssignmentLeftHandSide(): Boolean = (parent as? KtBinaryExpression)?.let { expression ->
    expression.operationToken == KtTokens.EQ && expression.left == this
} == true
