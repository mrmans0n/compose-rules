// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import java.net.URI

/**
 * Reports local `var` properties in composable scopes when they are not backed by observable state.
 */
class VarsWithoutStateBackingCheck(config: Config) :
    Rule(
        config,
        "Local var properties in composable scopes should be backed by observable state.",
        URI("https://mrmans0n.github.io/compose-rules/rules/#back-composable-vars-with-state"),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("VarsWithoutStateBacking")

    private var composableScopeDepth = 0

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (!function.isComposable()) {
            if (composableScopeDepth == 0) {
                super.visitNamedFunction(function)
            }
            return
        }

        composableScopeDepth++
        try {
            super.visitNamedFunction(function)
        } finally {
            composableScopeDepth--
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        if (!accessor.isGetter || !accessor.isComposable()) {
            if (composableScopeDepth == 0) {
                super.visitPropertyAccessor(accessor)
            }
            return
        }

        composableScopeDepth++
        try {
            super.visitPropertyAccessor(accessor)
        } finally {
            composableScopeDepth--
        }
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        if (lambdaExpression.isComposable()) {
            composableScopeDepth++
            try {
                super.visitLambdaExpression(lambdaExpression)
            } finally {
                composableScopeDepth--
            }
        } else if (composableScopeDepth == 0 || lambdaExpression.isEagerStdlibScopeFunctionLambda()) {
            super.visitLambdaExpression(lambdaExpression)
        } else {
            val previousComposableScopeDepth = composableScopeDepth
            composableScopeDepth = 0
            try {
                super.visitLambdaExpression(lambdaExpression)
            } finally {
                composableScopeDepth = previousComposableScopeDepth
            }
        }
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        if (composableScopeDepth == 0) {
            super.visitClassOrObject(classOrObject)
        }
    }

    override fun visitProperty(property: KtProperty) {
        if (composableScopeDepth > 0 && property.isVar && !property.hasComposeStateDelegate()) {
            reportVarsWithoutStateBacking(property)
        }
        super.visitProperty(property)
    }

    private fun reportVarsWithoutStateBacking(property: KtProperty) {
        report(
            Finding(
                entity = Entity.from(property),
                message = VarsWithoutStateBacking,
            ),
        )
    }

    internal companion object {
        val VarsWithoutStateBacking = """
            Local var properties in composable scopes should be backed by observable State.

            See https://mrmans0n.github.io/compose-rules/rules/#back-composable-vars-with-state for more information.
        """.trimIndent()
    }
}
