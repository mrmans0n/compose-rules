// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ParameterNaming

class ParameterNamingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ParameterNaming() {
    override val issue: Issue = Issue(
        id = "ParameterNaming",
        severity = Severity.CodeSmell,
        description = """
        Lambda parameters in a composable function should be in present tense, not past tense.

        Examples: `onClick` and not `onClicked`, `onTextChange` and not `onTextChanged`, etc.
        """.trimIndent(),
        debt = Debt.FIVE_MINS,
    )
}
