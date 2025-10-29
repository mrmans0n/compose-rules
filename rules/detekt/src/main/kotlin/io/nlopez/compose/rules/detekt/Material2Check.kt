// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.Material2
import java.net.URI

class Material2Check(config: Config) :
    DetektRule(
        config = config,
        description = "Material 2 composables should be migrated to Material 3",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#material-2"),
    ),
    ComposeKtVisitor by Material2()
