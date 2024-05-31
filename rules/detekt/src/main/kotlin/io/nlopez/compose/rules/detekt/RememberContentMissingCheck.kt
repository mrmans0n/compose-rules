// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.RememberContentMissing

class RememberContentMissingCheck(config: Config) :
    DetektRule(config, DESCRIPTION),
    ComposeKtVisitor by RememberContentMissing() {

    override val ruleId = Id("RememberContentMissing")

    private companion object {
        private const val DESCRIPTION = "Using movableContentOf/movableContentWithReceiverOf in a @Composable " +
            "function without it being remembered can cause visual problems, as the content would be recycled " +
            "when detached from the composition."
    }
}
