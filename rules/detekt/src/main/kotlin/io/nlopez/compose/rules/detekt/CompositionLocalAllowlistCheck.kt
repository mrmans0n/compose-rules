// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.CompositionLocalAllowlist
import io.nlopez.compose.rules.DetektRule
import java.net.URI

class CompositionLocalAllowlistCheck(config: Config) :
    DetektRule(
        config = config,
        description = "CompositionLocal usage should be from an allowlist",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#compositionlocal-allowlist"),
    ),
    ComposeKtVisitor by CompositionLocalAllowlist()
