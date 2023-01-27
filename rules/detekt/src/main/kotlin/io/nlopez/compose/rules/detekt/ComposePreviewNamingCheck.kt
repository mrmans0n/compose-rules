// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ComposePreviewNaming
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.TwitterDetektRule

class ComposePreviewNamingCheck(config: Config) :
    TwitterDetektRule(config),
    ComposeKtVisitor by ComposePreviewNaming() {

    override val issue: Issue = Issue(
        id = "PreviewNaming",
        severity = Severity.CodeSmell,
        description = "Multipreview annotations should end with the `Previews` suffix",
        debt = Debt.FIVE_MINS,
    )
}
