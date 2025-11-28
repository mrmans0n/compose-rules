// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ParameterNaming
import java.net.URI

class ParameterNamingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Parameters in Composable functions should follow proper naming conventions",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#parameter-naming"),
    ),
    ComposeKtVisitor by ParameterNaming()
