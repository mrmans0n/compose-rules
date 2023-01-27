// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ComposeModifierMissing
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.TwitterDetektRule

class ComposeModifierMissingCheck(config: Config) :
    TwitterDetektRule(config),
    ComposeKtVisitor by ComposeModifierMissing() {
    override val issue: Issue = Issue(
        id = "ModifierMissing",
        severity = Severity.Defect,
        description = ComposeModifierMissing.MissingModifierContentComposable,
        debt = Debt.TEN_MINS,
    )
}
