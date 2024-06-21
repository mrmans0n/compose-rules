// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierComposed

class ModifierComposedCheck(config: Config) :
    DetektRule(config, "Modifier composed check description"),
    ComposeKtVisitor by ModifierComposed() {

    override val ruleId = Id("ModifierComposed")
}
