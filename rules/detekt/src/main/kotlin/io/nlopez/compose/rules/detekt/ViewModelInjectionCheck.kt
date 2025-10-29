// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ViewModelInjection
import java.net.URI

class ViewModelInjectionCheck(config: Config) :
    DetektRule(
        config = config,
        description = "ViewModels should be created using dependency injection",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#viewmodel-injection"),
    ),
    ComposeKtVisitor by ViewModelInjection()
