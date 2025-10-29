// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.MultipleContentEmitters
import java.net.URI

class MultipleContentEmittersCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Composable functions should not emit multiple pieces of content",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#multiple-content-emitters"),
    ),
    ComposeKtVisitor by MultipleContentEmitters()
