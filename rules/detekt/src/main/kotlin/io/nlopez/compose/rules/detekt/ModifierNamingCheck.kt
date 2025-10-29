// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierNaming
import java.net.URI

class ModifierNamingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Modifier factory functions should be named consistently with the resulting modifier",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#modifier-naming"),
    ),
    ComposeKtVisitor by ModifierNaming()
