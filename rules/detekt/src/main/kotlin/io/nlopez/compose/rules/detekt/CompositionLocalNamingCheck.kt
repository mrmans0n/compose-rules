// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.CompositionLocalNaming
import io.nlopez.compose.rules.DetektRule

class CompositionLocalNamingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by CompositionLocalNaming() {

    override val issue: Issue = Issue(
        id = "CompositionLocalNaming",
        severity = Severity.CodeSmell,
        description = CompositionLocalNaming.CompositionLocalNeedsLocalPrefix,
        debt = Debt.FIVE_MINS,
    )
}
