// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.PreviewNaming
import java.net.URI

class PreviewNamingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Preview composables should be named appropriately",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#preview-naming"),
    ),
    ComposeKtVisitor by PreviewNaming()
