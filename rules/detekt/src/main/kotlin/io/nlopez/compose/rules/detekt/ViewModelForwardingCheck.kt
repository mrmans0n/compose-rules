// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ViewModelForwarding
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class ViewModelForwardingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ViewModelForwarding() {
    override val issue: Issue = Issue(
        id = "ViewModelForwarding",
        severity = Severity.CodeSmell,
        description = ViewModelForwarding.AvoidViewModelForwarding,
        debt = Debt.TWENTY_MINS,
    )
}
