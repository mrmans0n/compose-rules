// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.MutableStateAutoboxing
import java.net.URI

class MutableStateAutoboxingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Avoid autoboxing when creating MutableState",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#mutable-state-autoboxing"),
    ),
    ComposeKtVisitor by MutableStateAutoboxing()
