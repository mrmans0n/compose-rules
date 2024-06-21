// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.Material2

class Material2Check(config: Config) :
    DetektRule(config, Material2.DisallowedUsageOfMaterial2),
    ComposeKtVisitor by Material2() {

    override val ruleId = Id("Material2")
}
