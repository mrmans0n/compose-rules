// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ComposeContentEmitterReturningValues
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.TwitterDetektRule

class ComposeContentEmitterReturningValuesCheck(config: Config) :
    TwitterDetektRule(config),
    ComposeKtVisitor by ComposeContentEmitterReturningValues() {

    override val issue: Issue = Issue(
        id = "ContentEmitterReturningValues",
        severity = Severity.Defect,
        description = ComposeContentEmitterReturningValues.ContentEmitterReturningValuesToo,
        debt = Debt.TWENTY_MINS,
    )
}
