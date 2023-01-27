// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ComposeCompositionLocalAllowlist
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.TwitterDetektRule

class ComposeCompositionLocalAllowlistCheck(config: Config) :
    TwitterDetektRule(config),
    ComposeKtVisitor by ComposeCompositionLocalAllowlist() {

    override val issue: Issue = Issue(
        id = "CompositionLocalAllowlist",
        severity = Severity.CodeSmell,
        description = ComposeCompositionLocalAllowlist.CompositionLocalNotInAllowlist,
        debt = Debt.FIVE_MINS,
    )
}
