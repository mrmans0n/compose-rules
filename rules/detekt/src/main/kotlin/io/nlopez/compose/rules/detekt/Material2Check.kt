// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.Material2

class Material2Check(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by Material2() {
    override val issue: Issue = Issue(
        id = "Material2",
        severity = Severity.Maintainability,
        description = Material2.DisallowedUsageOfMaterial2,
        debt = Debt.TEN_MINS,
    )
}
