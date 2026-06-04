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
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtUnaryExpression
import java.net.URI

/**
 * Reports composable declarations that call only read-only composables, and can therefore be marked
 * `@ReadOnlyComposable`.
 */
class MissingReadOnlyComposableCheck(config: Config) :
    Rule(
        config,
        "Composable declarations that only call read-only composables should be marked as @ReadOnlyComposable.",
        URI("https://mrmans0n.github.io/compose-rules/rules/#mark-read-only-composables-as-read-only"),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("MissingReadOnlyComposable")

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isComposable()) return
        if (function.isReadOnlyComposable()) return
        if (function.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
        if (function.bodyExpression == null) return
        if ((function.bodyExpression as? KtBlockExpression)?.statements?.isEmpty() == true) return

        if (function.bodyExpression!!.readOnlyComposableUsage().canBeReadOnlyComposable) {
            reportMissingReadOnlyComposable(function)
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        super.visitPropertyAccessor(accessor)
        if (!accessor.isGetter) return
        if (!accessor.isComposable()) return
        if (accessor.isReadOnlyComposable()) return
        if (accessor.property.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
        if (accessor.bodyExpression == null) return

        if (accessor.bodyExpression!!.readOnlyComposableUsage().canBeReadOnlyComposable) {
            reportMissingReadOnlyComposable(accessor)
        }
    }

    private fun reportMissingReadOnlyComposable(element: KtElement) {
        report(
            Finding(
                Entity.from(element),
                MissingReadOnlyComposable,
            ),
        )
    }

    private fun KtElement.readOnlyComposableUsage(): ReadOnlyComposableUsage {
        var hasReadOnlyComposableUsage = false
        var hasNonReadOnlyComposableUsage = false

        accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.isComposableCall()) {
                        if (expression.isReadOnlyComposableCall()) {
                            hasReadOnlyComposableUsage = true
                        } else {
                            hasNonReadOnlyComposableUsage = true
                            return
                        }
                    } else if (!expression.isEagerScopeFunctionCall()) {
                        hasNonReadOnlyComposableUsage = true
                        return
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    if (expression.isComposablePropertyRead()) {
                        if (expression.isReadOnlyComposablePropertyRead()) {
                            hasReadOnlyComposableUsage = true
                        } else {
                            hasNonReadOnlyComposableUsage = true
                            return
                        }
                    } else if (expression.isPropertyReadWithExecutableAccess()) {
                        hasNonReadOnlyComposableUsage = true
                        return
                    }
                    super.visitSimpleNameExpression(expression)
                }

                override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                    if (expression.isCompositionLocalCurrentRead()) {
                        hasReadOnlyComposableUsage = true
                        expression.receiverExpression.accept(this)
                        return
                    }
                    super.visitDotQualifiedExpression(expression)
                }

                override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
                    hasNonReadOnlyComposableUsage = true
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    hasNonReadOnlyComposableUsage = true
                }

                override fun visitUnaryExpression(expression: KtUnaryExpression) {
                    if (expression.operationToken in IncrementOrDecrementOperators) {
                        hasNonReadOnlyComposableUsage = true
                        return
                    }
                    super.visitUnaryExpression(expression)
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

        return ReadOnlyComposableUsage(
            hasReadOnlyComposableUsage = hasReadOnlyComposableUsage,
            hasNonReadOnlyComposableUsage = hasNonReadOnlyComposableUsage,
        )
    }

    internal companion object {
        val MissingReadOnlyComposable = """
            This @Composable declaration only calls read-only composables and should be marked @ReadOnlyComposable.

            See https://mrmans0n.github.io/compose-rules/rules/#mark-read-only-composables-as-read-only for more information.
        """.trimIndent()
    }
}

private val IncrementOrDecrementOperators = setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS)

private data class ReadOnlyComposableUsage(
    val hasReadOnlyComposableUsage: Boolean,
    val hasNonReadOnlyComposableUsage: Boolean,
) {
    val canBeReadOnlyComposable: Boolean
        get() = hasReadOnlyComposableUsage && !hasNonReadOnlyComposableUsage
}
