// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.MultipleContentEmitters

class MultipleContentEmittersCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by MultipleContentEmitters() {

    override val issue: Issue = Issue(
        id = "MultipleEmitters",
        severity = Severity.Defect,
        description = MultipleContentEmitters.MultipleContentEmittersDetected,
        debt = Debt.TWENTY_MINS,
    )
}
