// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.ComposeMultipleContentEmitters
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class ComposeMultipleContentEmittersCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ComposeMultipleContentEmitters() {

    override val issue: Issue = Issue(
        id = "MultipleEmitters",
        severity = Severity.Defect,
        description = ComposeMultipleContentEmitters.MultipleContentEmittersDetected,
        debt = Debt.TWENTY_MINS,
    )
}
