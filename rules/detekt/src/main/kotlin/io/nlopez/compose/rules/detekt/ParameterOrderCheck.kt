// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ParameterOrder
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class ParameterOrderCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ParameterOrder() {
    override val issue: Issue = Issue(
        id = "ComposableParamOrder",
        severity = Severity.CodeSmell,
        description = "Parameters in a composable function should be ordered following this pattern: " +
            "params without defaults, modifiers, params with defaults and optionally, " +
            "a trailing function that might not have a default param.",
        debt = Debt.TEN_MINS,
    )
}
