// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.PreviewPublic
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class PreviewPublicCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by PreviewPublic() {

    override val issue: Issue = Issue(
        id = "PreviewPublic",
        severity = Severity.CodeSmell,
        description = PreviewPublic.ComposablesPreviewShouldNotBePublic,
        debt = Debt.FIVE_MINS,
    )
}
