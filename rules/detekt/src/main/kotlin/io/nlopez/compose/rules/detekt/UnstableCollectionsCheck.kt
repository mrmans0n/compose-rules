// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.UnstableCollections
import java.net.URI

class UnstableCollectionsCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Avoid using unstable collections in Composable function signatures",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#unstable-collections"),
    ),
    ComposeKtVisitor by UnstableCollections()
