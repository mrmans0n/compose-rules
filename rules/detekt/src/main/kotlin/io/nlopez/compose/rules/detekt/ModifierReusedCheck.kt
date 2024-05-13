// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierReused

class ModifierReusedCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ModifierReused() {
    override val issue: Issue = Issue(
        id = "ModifierReused",
        severity = Severity.Defect,
        description = ModifierReused.ModifierShouldBeUsedOnceOnly,
        debt = Debt.TWENTY_MINS,
    )
}
