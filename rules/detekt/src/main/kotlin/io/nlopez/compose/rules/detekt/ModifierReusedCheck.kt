// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierReused
import java.net.URI

class ModifierReusedCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Modifiers should not be reused",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#modifier-reused"),
    ),
    ComposeKtVisitor by ModifierReused()
