// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.UnstableCollections

class UnstableCollectionsCheck(config: Config) :
    DetektRule(config, description),
    ComposeKtVisitor by UnstableCollections() {

    override val ruleId = Id("UnstableCollections")

    private companion object {
        private val description = """
            The Compose Compiler cannot infer the stability of a parameter if a List/Set/Map is used in it, even if the item type is stable.
            You should use Kotlinx Immutable Collections instead, or create an `@Immutable` wrapper for this class.

            See https://mrmans0n.github.io/compose-rules/rules/#avoid-using-unstable-collections for more information.
        """.trimIndent()
    }
}
