// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ComposableNestingDepth
import io.nlopez.compose.rules.DetektRule
import java.net.URI

class ComposableNestingDepthCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Limit how deeply content emitters can be nested inside a @Composable",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#avoid-deeply-nested-composables"),
    ),
    ComposeKtVisitor by ComposableNestingDepth()
