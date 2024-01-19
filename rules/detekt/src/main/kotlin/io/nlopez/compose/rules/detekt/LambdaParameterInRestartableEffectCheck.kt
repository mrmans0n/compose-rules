// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.LambdaParameterInRestartableEffect
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class LambdaParameterInRestartableEffectCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by LambdaParameterInRestartableEffect() {

    override val issue: Issue = Issue(
        id = "LambdaParameterInRestartableEffect",
        severity = Severity.Defect,
        description = LambdaParameterInRestartableEffect.LambdaUsedInRestartableEffect,
        debt = Debt.TWENTY_MINS,
    )
}
