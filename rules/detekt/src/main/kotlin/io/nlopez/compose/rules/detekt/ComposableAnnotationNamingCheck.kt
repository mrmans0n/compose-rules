// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ComposableAnnotationNaming
import io.nlopez.compose.rules.DetektRule
import java.net.URI

class ComposableAnnotationNamingCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Composable annotation classes should be properly named",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#composable-annotation-naming"),
    ),
    ComposeKtVisitor by ComposableAnnotationNaming()
