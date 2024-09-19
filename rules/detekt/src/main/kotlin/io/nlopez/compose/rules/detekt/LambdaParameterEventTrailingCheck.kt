// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.LambdaParameterEventTrailing

class LambdaParameterEventTrailingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by LambdaParameterEventTrailing() {

    override val issue: Issue = Issue(
        id = "LambdaParameterEventTrailing",
        severity = Severity.Style,
        description = LambdaParameterEventTrailing.EventLambdaIsTrailingLambda,
        debt = Debt.FIVE_MINS,
    )
}
