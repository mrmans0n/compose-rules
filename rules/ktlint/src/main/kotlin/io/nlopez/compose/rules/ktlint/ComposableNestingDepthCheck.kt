// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.rules.ComposableNestingDepth
import io.nlopez.compose.rules.KtlintRule
import org.jetbrains.kotlin.psi.KtFunction

class ComposableNestingDepthCheck :
    KtlintRule(
        id = "compose:composable-nesting-depth-check",
        editorConfigProperties = setOf(
            composableNestingDepthEnabled,
            composableNestingDepthThreshold,
            contentEmittersProperty,
            contentEmittersDenylist,
        ),
    ),
    ComposeKtVisitor {

    private val visitor = ComposableNestingDepth()

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        // ktlint allows all rules by default, so we'll add an extra param to make sure it's disabled by default
        if (config.getBoolean("composableNestingDepthEnabled", false)) {
            visitor.visitComposable(function, emitter, config)
        }
    }
}
