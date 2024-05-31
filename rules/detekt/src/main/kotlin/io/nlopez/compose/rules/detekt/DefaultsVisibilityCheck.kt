// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DefaultsVisibility
import io.nlopez.compose.rules.DetektRule

class DefaultsVisibilityCheck(config: Config) :
    DetektRule(config, "@Composable `Defaults` objects should match visibility of the composables they serve."),
    ComposeKtVisitor by DefaultsVisibility() {

    override val ruleId = Id("DefaultsVisibility")
}
