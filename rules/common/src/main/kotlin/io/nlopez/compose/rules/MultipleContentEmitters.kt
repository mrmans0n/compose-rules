// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.createDirectComposableToEmissionCountMapping
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.hasReceiverType
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.refineComposableToEmissionCountMapping
import org.jetbrains.kotlin.psi.KtFile
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
        val composableToEmissionCount = with(config) { composables.createDirectComposableToEmissionCountMapping() }

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
        val currentMapping = with(config) { refineComposableToEmissionCountMapping(composableToEmissionCount) }

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

        val MultipleContentEmittersDetected = """
            Composable functions should only be emitting content into the composition from one source at their top level.

            See https://mrmans0n.github.io/compose-rules/rules/#do-not-emit-multiple-pieces-of-content for more information.
        """.trimIndent()
    }
}
