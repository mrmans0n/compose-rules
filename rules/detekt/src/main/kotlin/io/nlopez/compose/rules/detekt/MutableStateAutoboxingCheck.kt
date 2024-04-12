// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.MutableStateAutoboxing
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class MutableStateAutoboxingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by MutableStateAutoboxing() {
    override val issue: Issue = Issue(
        id = "MutableStateAutoboxing",
        severity = Severity.Performance,
        description = "Using mutableInt/Long/Double/FloatStateOf is recommended over mutableStateOf<X> for " +
            "Int/Long/Double/Float, as it uses the primitives directly which is more performant.",
        debt = Debt.FIVE_MINS,
    )
}
