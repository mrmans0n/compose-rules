// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import java.net.URI

/**
 * Reports `@ReadOnlyComposable` declarations that make composable calls which are not also
 * `@ReadOnlyComposable`.
 */
class InvalidReadOnlyComposableCheck(config: Config) :
    Rule(
        config,
        "Functions marked as @ReadOnlyComposable should only call other read-only composables.",
        URI("https://mrmans0n.github.io/compose-rules/rules/#do-not-mark-composables-that-emit-content-as-read-only"),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("InvalidReadOnlyComposable")

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isComposable() || !function.isReadOnlyComposable()) return

        function.bodyExpression?.findFirstNonReadOnlyComposableCall()?.let { call ->
            reportInvalidReadOnlyComposable(call)
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        super.visitPropertyAccessor(accessor)
        if (!accessor.isGetter || !accessor.isComposable() || !accessor.isReadOnlyComposable()) return

        accessor.bodyExpression?.findFirstNonReadOnlyComposableCall()?.let { call ->
            reportInvalidReadOnlyComposable(call)
        }
    }

    private fun reportInvalidReadOnlyComposable(call: KtCallExpression) {
        report(
            Finding(
                Entity.from(call),
                InvalidReadOnlyComposable,
            ),
        )
    }

    private fun reportInvalidReadOnlyComposable(expression: KtSimpleNameExpression) {
        report(
            Finding(
                Entity.from(expression),
                InvalidReadOnlyComposable,
            ),
        )
    }

    private fun KtElement.findFirstNonReadOnlyComposableCall(): KtCallExpression? {
        var firstInvalidCall: KtCallExpression? = null
        var firstInvalidPropertyRead: KtSimpleNameExpression? = null
        accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (firstInvalidCall != null || firstInvalidPropertyRead != null) return
                    if (expression.isComposableCall() && !expression.isReadOnlyComposableCall()) {
                        firstInvalidCall = expression
                        return
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    if (firstInvalidCall != null || firstInvalidPropertyRead != null) return
                    if (
                        expression.isComposablePropertyRead() &&
                        !expression.isReadOnlyComposablePropertyRead()
                    ) {
                        firstInvalidPropertyRead = expression
                        return
                    }
                    super.visitSimpleNameExpression(expression)
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    // Nested function bodies are not evaluated by the read-only declaration.
                }

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                    // Nested accessor bodies are not evaluated by the read-only declaration.
                }

                override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                    if (lambdaExpression.isEagerScopeFunctionLambda()) {
                        super.visitLambdaExpression(lambdaExpression)
                    }
                }
            },
        )
        firstInvalidPropertyRead?.let { reportInvalidReadOnlyComposable(it) }
        return firstInvalidCall
    }

    internal companion object {
        val InvalidReadOnlyComposable = """
            This @ReadOnlyComposable declaration calls a composable that is not read-only.

            See https://mrmans0n.github.io/compose-rules/rules/#do-not-mark-composables-that-emit-content-as-read-only for more information.
        """.trimIndent()
    }
}
