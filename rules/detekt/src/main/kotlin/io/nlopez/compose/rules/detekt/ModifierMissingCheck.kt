// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.config
import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.isAnnotatedWith
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierMissing
import org.jetbrains.kotlin.psi.KtFunction
import java.net.URI

class ModifierMissingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Composable functions should accept a Modifier parameter",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#modifier-missing"),
    ) {
    private val visitor: ComposeKtVisitor = ModifierMissing()

    // On the docs it looks like this is a common suppressor that should be available everywhere,
    // but it doesn't seem to be (according to unit tests). Oh well, I guess I'll just leave the extra check for now.
    private val ignoreAnnotated: List<String> by config(emptyList<String>()) { list -> list.map(String::trim) }

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        if (function.isAnnotatedWith(ignoreAnnotated.toSet())) return
        visitor.visitComposable(function, emitter, config)
    }
}
