// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.CompositionLocalNaming
import io.nlopez.compose.rules.DetektRule
import java.net.URI

class CompositionLocalNamingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "CompositionLocal properties should be prefixed with 'Local'",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#compositionlocal-naming"),
    ),
    ComposeKtVisitor by CompositionLocalNaming()
