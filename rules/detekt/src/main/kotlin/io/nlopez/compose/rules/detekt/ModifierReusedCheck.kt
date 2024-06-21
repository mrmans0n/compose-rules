// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierReused

class ModifierReusedCheck(config: Config) :
    DetektRule(config, ModifierReused.ModifierShouldBeUsedOnceOnly),
    ComposeKtVisitor by ModifierReused() {

    override val ruleId = Id("ModifierReused")
}
