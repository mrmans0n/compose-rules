// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierMissing

class ModifierMissingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ModifierMissing() {
    override val issue: Issue = Issue(
        id = "ModifierMissing",
        severity = Severity.Defect,
        description = ModifierMissing.MissingModifierContentComposable,
        debt = Debt.TEN_MINS,
    )
}
