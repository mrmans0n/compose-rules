// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ContentEmitterReturningValues
import io.nlopez.compose.rules.DetektRule
import java.net.URI

class ContentEmitterReturningValuesCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Composable functions that emit content should not return values",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#content-emitter-returning-values"),
    ),
    ComposeKtVisitor by ContentEmitterReturningValues()
