// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.LambdaParameterInRestartableEffect

class LambdaParameterInRestartableEffectCheck(config: Config) :
    DetektRule(config, LambdaParameterInRestartableEffect.LambdaUsedInRestartableEffect),
    ComposeKtVisitor by LambdaParameterInRestartableEffect() {

    override val ruleId = Id("LambdaParameterInRestartableEffect")
}
