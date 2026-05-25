// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.emitsContent
import io.nlopez.compose.core.util.findAllChildrenByClass
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.parents

class ComposableNestingDepth : ComposeKtVisitor {
    override val isOptIn: Boolean = true

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        val body = function.bodyBlockExpression ?: return
        val threshold = config.getInt("composableNestingDepthThreshold", 3)

        // Exclude calls that live inside a nested function or class dec, then
        // For each content-emitting call in this function, count how many enclosing content-emitting,
        // calls it has (its nesting level). Top-level emitters have nesting 0.
        val deepest = body.findAllChildrenByClass<KtCallExpression>()
            .filter { call ->
                call.parents
                    .takeWhile { it != body }
                    .none { it is KtNamedFunction || it is KtClassOrObject }
            }
            .filter { it.emitsContent(config) }
            .maxOfOrNull { call ->
                call.parents
                    .takeWhile { it != body }
                    .filterIsInstance<KtCallExpression>()
                    .count { it.emitsContent(config) }
            } ?: return

        if (deepest > threshold) {
            emitter.report(function, ComposableTooDeeplyNested)
        }
    }

    companion object {

        val ComposableTooDeeplyNested = """
            This @Composable function nests content emitters more deeply than the configured threshold. Extract inner sections into dedicated private @Composable functions to keep the structure readable.
            See https://mrmans0n.github.io/compose-rules/rules/#avoid-deeply-nested-composables for more information.
        """.trimIndent()
    }
}
