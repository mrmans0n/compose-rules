// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ComposeModifierComposable
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.TwitterDetektRule

class ComposeModifierComposableCheck(config: Config) :
    TwitterDetektRule(config),
    ComposeKtVisitor by ComposeModifierComposable() {
    override val issue: Issue = Issue(
        id = "ModifierComposable",
        severity = Severity.Performance,
        description = ComposeModifierComposable.ComposableModifier,
        debt = Debt.TEN_MINS,
    )
}
