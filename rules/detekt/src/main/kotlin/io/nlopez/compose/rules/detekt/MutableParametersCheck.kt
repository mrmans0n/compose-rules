// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.MutableParameters
import java.net.URI

class MutableParametersCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Composable function parameters should be immutable",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#mutable-parameters"),
    ),
    ComposeKtVisitor by MutableParameters()
