// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.RememberContentMissing
import java.net.URI

class RememberContentMissingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Content-producing composables should be remembered",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#remember-content-missing"),
    ),
    ComposeKtVisitor by RememberContentMissing()
