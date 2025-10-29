// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierWithoutDefault
import java.net.URI

class ModifierWithoutDefaultCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Modifier parameters should have a default value",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#modifier-without-default"),
    ),
    ComposeKtVisitor by ModifierWithoutDefault()
