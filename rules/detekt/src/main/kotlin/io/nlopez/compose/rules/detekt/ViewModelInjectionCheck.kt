// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ViewModelInjection
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class ViewModelInjectionCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ViewModelInjection() {

    override val issue: Issue = Issue(
        id = "ViewModelInjection",
        severity = Severity.CodeSmell,
        description = """
            Implicit dependencies of composables should be made explicit.

            Acquiring a ViewModel should be done in composable default parameters, so that it is more testable and flexible.
        """.trimIndent(),
        debt = Debt.TEN_MINS,
    )
}
