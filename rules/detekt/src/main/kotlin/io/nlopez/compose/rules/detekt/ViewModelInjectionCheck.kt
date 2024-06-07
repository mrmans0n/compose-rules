// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ViewModelInjection

class ViewModelInjectionCheck(config: Config) :
    DetektRule(config, "Implicit dependencies of composables should be made explicit."),
    ComposeKtVisitor by ViewModelInjection() {

    override val ruleId = Id("ViewModelInjection")
}
