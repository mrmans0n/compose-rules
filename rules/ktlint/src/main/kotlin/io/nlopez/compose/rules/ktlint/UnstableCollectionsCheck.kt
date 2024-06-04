// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.rules.KtlintRule
import io.nlopez.compose.rules.UnstableCollections
import org.jetbrains.kotlin.psi.KtFunction

class UnstableCollectionsCheck :
    KtlintRule(
        id = "compose:unstable-collections",
        editorConfigProperties = setOf(disallowUnstableCollections),
    ),
    ComposeKtVisitor {

    private val visitor = UnstableCollections()

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        // ktlint allows all rules by default, so we'll add an extra param to make sure it's disabled by default
        if (config.getBoolean("disallowUnstableCollections", false)) {
            visitor.visitComposable(function, emitter, config)
        }
    }
}
