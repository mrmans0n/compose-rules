// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.CompositionLocalNaming
import io.nlopez.compose.rules.DetektRule

class CompositionLocalNamingCheck(config: Config) :
    DetektRule(config, CompositionLocalNaming.CompositionLocalNeedsLocalPrefix),
    ComposeKtVisitor by CompositionLocalNaming() {

    override val ruleId = Id("CompositionLocalNaming")
}
