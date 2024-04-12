// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.Material2
import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.ktlint.KtlintRule
import org.jetbrains.kotlin.psi.KtFile

class Material2Check :
    KtlintRule(
        id = "compose:material-two",
        editorConfigProperties = setOf(allowedFromM2, disallowMaterial2),
    ),
    ComposeKtVisitor {
    private val visitor = Material2()

    override fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {
        // ktlint allows all rules by default, so we'll add an extra param to make sure it's disabled by default
        if (config.getBoolean("disallowMaterial2", false)) {
            visitor.visitFile(file, autoCorrect, emitter, config)
        }
    }
}
