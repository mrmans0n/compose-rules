// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.emitsContent
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.hasReceiverType
import io.nlopez.rules.core.util.isComposable
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction

class MultipleContentEmitters : ComposeKtVisitor {

    override fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {
        // CHECK #1 : We want to find the composables first that are at risk of emitting content from multiple sources.
        val composables = file.findChildrenByClass<KtFunction>()
            .filter { it.isComposable }
            // We don't want to analyze composables that are extension functions, as they might be things like
            // BoxScope which are legit, and we want to avoid false positives.
            .filter { it.hasBlockBody() }
            // Same applies to context receivers: we could have a BoxScope/ColumnScope/RowScope and it'd be legit.
            // We don't have a way to know for sure, so we'd better avoid the issue altogether.
            .filter { it.contextReceivers.isEmpty() }
            // We want only methods with a body
            .filterNot { it.hasReceiverType }

        // Now we want to get the count of direct emitters in them: the composables we know for a fact that output UI
        val composableToEmissionCount = composables.associateWith { with(config) { it.directUiEmitterCount } }

        // We can start showing errors, for composables that emit more than once (from the list of known composables)
        val directEmissionsReported = composableToEmissionCount.filterValues { it > 1 }.keys
        for (composable in directEmissionsReported) {
            emitter.report(composable, MultipleContentEmittersDetected)
        }

        // Now we can give some extra passes through the list of composables, and try to get a more accurate count.
        // We want to make sure that if these composables are using other composables in this file that emit UI,
        // those are taken into account too. For example:
        // @Composable fun Comp1() { Text("Hi") }
        // @Composable fun Comp2() { Text("Hola") }
        // @Composable fun Comp3() { Comp1() Comp2() } // This wouldn't be picked up at first, but should after 1 loop
        var currentMapping = composableToEmissionCount

        var shouldMakeAnotherPass = true
        while (shouldMakeAnotherPass) {
            val updatedMapping = currentMapping.mapValues { (functionNode, _) ->
                with(config) { functionNode.indirectUiEmitterCount(currentMapping) }
            }
            when {
                updatedMapping != currentMapping -> currentMapping = updatedMapping
                else -> shouldMakeAnotherPass = false
            }
        }

        // Here we have the settled data after all the needed passes, so we want to show errors for them,
        // if they were not caught already by the 1st emission loop
        currentMapping.filterValues { it > 1 }
            .filterNot { directEmissionsReported.contains(it.key) }
            .keys
            .forEach { composable ->
                emitter.report(composable, MultipleContentEmittersDetected)
            }
    }

    companion object {

        context(ComposeKtConfig)
        internal val KtFunction.directUiEmitterCount: Int
            get() = bodyBlockExpression?.let { block ->
                // If there's content emitted in a for loop, we assume there's at
                // least two iterations and thus count any emitters in them as multiple
                val forLoopCount = when {
                    block.forLoopHasUiEmitters -> 2
                    else -> 0
                }
                block.directUiEmitterCount + forLoopCount
            } ?: 0

        context(ComposeKtConfig)
        internal val KtBlockExpression.forLoopHasUiEmitters: Boolean
            get() = statements.filterIsInstance<KtForExpression>().any {
                when (val body = it.body) {
                    is KtBlockExpression -> body.directUiEmitterCount > 0
                    is KtCallExpression -> body.emitsContent
                    else -> false
                }
            }

        context(ComposeKtConfig)
        internal val KtBlockExpression.directUiEmitterCount: Int
            get() = statements.filterIsInstance<KtCallExpression>().count { it.emitsContent }

        context(ComposeKtConfig)
        internal fun KtFunction.indirectUiEmitterCount(mapping: Map<KtFunction, Int>): Int {
            val bodyBlock = bodyBlockExpression ?: return 0
            return bodyBlock.statements
                .filterIsInstance<KtCallExpression>()
                .count { callExpression ->
                    // If it's a direct hit on our list, it should count directly
                    if (callExpression.emitsContent) return@count true

                    val name = callExpression.calleeExpression?.text ?: return@count false
                    // If the hit is in the provided mapping, it means it is using a composable that we know emits UI,
                    // that we inferred from previous passes
                    val value = mapping.mapKeys { entry -> entry.key.name }.getOrElse(name) { return@count false }
                    value > 0
                }
        }

        val MultipleContentEmittersDetected = """
            Composable functions should only be emitting content into the composition from one source at their top level.

            See https://mrmans0n.github.io/compose-rules/rules/#do-not-emit-multiple-pieces-of-content for more information.
        """.trimIndent()
    }
}
