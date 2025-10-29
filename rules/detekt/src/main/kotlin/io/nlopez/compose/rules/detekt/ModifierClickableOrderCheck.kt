// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierClickableOrder
import java.net.URI

class ModifierClickableOrderCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Modifier.clickable should be applied before other modifiers",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#modifier-clickable-order"),
    ),
    ComposeKtVisitor by ModifierClickableOrder()
