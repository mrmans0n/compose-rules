// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.RememberContentMissing
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class RememberContentMissingCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by RememberContentMissing() {

    override val issue: Issue = Issue(
        id = "RememberContentMissing",
        severity = Severity.Defect,
        description = """
            Using movableContentOf/movableContentWithReceiverOf in a @Composable function without it being remembered can cause visual problems, as the content would be recycled when detached from the composition.
        """.trimIndent(),
        debt = Debt.FIVE_MINS,
    )
}
