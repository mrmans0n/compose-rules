// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ContentTrailingLambda
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class ContentTrailingLambdaCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ContentTrailingLambda() {
    override val issue: Issue = Issue(
        id = "ContentTrailingLambda",
        severity = Severity.CodeSmell,
        description = ContentTrailingLambda.ContentShouldBeTrailingLambda,
        debt = Debt.TEN_MINS,
    )
}
