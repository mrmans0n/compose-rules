// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ContentSlotReused
import io.nlopez.compose.rules.DetektRule
import java.net.URI

class ContentSlotReusedCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Content lambda parameters should not be invoked more than once",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#content-slot-reused"),
    ),
    ComposeKtVisitor by ContentSlotReused()
