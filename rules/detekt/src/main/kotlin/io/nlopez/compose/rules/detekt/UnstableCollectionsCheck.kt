// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.rules.UnstableCollections
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.detekt.DetektRule

class UnstableCollectionsCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by UnstableCollections() {
    override val issue: Issue = Issue(
        id = "UnstableCollections",
        severity = Severity.Defect,
        description = """
            The Compose Compiler cannot infer the stability of a parameter if a List/Set/Map is used in it, even if the item type is stable.
            You should use Kotlinx Immutable Collections instead, or create an `@Immutable` wrapper for this class.

            See https://mrmans0n.github.io/compose-rules/rules/#avoid-using-unstable-collections for more information.
        """.trimIndent(),
        debt = Debt.TWENTY_MINS,
    )
}
