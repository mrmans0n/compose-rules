// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.DefaultsVisibility
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class DefaultsVisibilityCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by DefaultsVisibility() {
    override val issue: Issue = Issue(
        id = "DefaultsVisibility",
        severity = Severity.Defect,
        description = "@Composable `Defaults` objects should match visibility of the composables they serve.",
        debt = Debt.TEN_MINS,
    )
}
