// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.UnstableCollections
import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.ktlint.KtlintRule
import org.jetbrains.kotlin.psi.KtFunction

class UnstableCollectionsCheck :
    KtlintRule(
        id = "compose:unstable-collections",
        editorConfigProperties = setOf(disallowUnstableCollections),
    ),
    ComposeKtVisitor {

    private val visitor = UnstableCollections()

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) {
        // ktlint allows all rules by default, so we'll add an extra param to make sure it's disabled by default
        if (config.getBoolean("disallowUnstableCollections", false)) {
            visitor.visitComposable(function, autoCorrect, emitter, config)
        }
    }
}
