// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ComposePreviewAnnotationNaming
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class ComposePreviewAnnotationNamingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ComposePreviewAnnotationNaming() {

    override val issue: Issue = Issue(
        id = "PreviewAnnotationNaming",
        severity = Severity.CodeSmell,
        description = "Multipreview annotations should begin with the `Preview` suffix",
        debt = Debt.FIVE_MINS,
    )
}
