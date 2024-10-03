// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ContentSlotReused
import io.nlopez.compose.rules.DetektRule

class ContentSlotReusedCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ContentSlotReused() {
    override val issue: Issue = Issue(
        id = "ContentSlotReused",
        severity = Severity.Defect,
        description = ContentSlotReused.ContentSlotsShouldNotBeReused,
        debt = Debt.TEN_MINS,
    )
}
