// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ComposableAnnotationNaming
import io.nlopez.compose.rules.DetektRule

class ComposableAnnotationNamingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ComposableAnnotationNaming() {

    override val issue: Issue = Issue(
        id = "ComposableAnnotationNaming",
        severity = Severity.CodeSmell,
        description = ComposableAnnotationNaming.ComposableAnnotationDoesNotEndWithComposable,
        debt = Debt.FIVE_MINS,
    )
}
