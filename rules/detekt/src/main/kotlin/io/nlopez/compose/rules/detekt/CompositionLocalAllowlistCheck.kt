// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.CompositionLocalAllowlist
import io.nlopez.compose.rules.DetektRule

class CompositionLocalAllowlistCheck(config: Config) :
    DetektRule(config, CompositionLocalAllowlist.CompositionLocalNotInAllowlist),
    ComposeKtVisitor by CompositionLocalAllowlist() {

    override val ruleId = Id("CompositionLocalAllowlist")
}
