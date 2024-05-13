// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ContentEmitterReturningValues
import io.nlopez.compose.rules.DetektRule

class ContentEmitterReturningValuesCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ContentEmitterReturningValues() {

    override val issue: Issue = Issue(
        id = "ContentEmitterReturningValues",
        severity = Severity.Defect,
        description = ContentEmitterReturningValues.ContentEmitterReturningValuesToo,
        debt = Debt.TWENTY_MINS,
    )
}
