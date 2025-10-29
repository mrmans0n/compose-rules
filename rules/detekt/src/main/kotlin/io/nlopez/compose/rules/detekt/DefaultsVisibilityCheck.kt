// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DefaultsVisibility
import io.nlopez.compose.rules.DetektRule
import java.net.URI

class DefaultsVisibilityCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Composable function defaults should have appropriate visibility",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#defaults-visibility"),
    ),
    ComposeKtVisitor by DefaultsVisibility()
