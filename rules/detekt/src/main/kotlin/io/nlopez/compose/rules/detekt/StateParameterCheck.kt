// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.StateParameter
import java.net.URI

class StateParameterCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Avoid passing State as a parameter",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#state-parameter"),
    ),
    ComposeKtVisitor by StateParameter()
