// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.LambdaParameterEventTrailing
import java.net.URI

class LambdaParameterEventTrailingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Event lambda parameters should be placed at the end of parameter list",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#lambda-parameter-event-trailing"),
    ),
    ComposeKtVisitor by LambdaParameterEventTrailing()
