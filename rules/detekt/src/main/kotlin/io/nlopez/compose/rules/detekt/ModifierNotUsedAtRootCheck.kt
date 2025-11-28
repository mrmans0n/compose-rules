// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierNotUsedAtRoot
import java.net.URI

class ModifierNotUsedAtRootCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Modifier parameters should be used at the root level composable",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#modifier-not-used-at-root"),
    ),
    ComposeKtVisitor by ModifierNotUsedAtRoot()
