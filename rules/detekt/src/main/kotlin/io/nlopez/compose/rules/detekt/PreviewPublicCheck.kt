// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.PreviewPublic
import java.net.URI

class PreviewPublicCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Preview composables should not be public",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#preview-public"),
    ),
    ComposeKtVisitor by PreviewPublic()
