// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ViewModelInjection

class ViewModelInjectionCheck(config: Config) :
    DetektRule(config, description),
    ComposeKtVisitor by ViewModelInjection() {

    override val ruleId = Id("ViewModelInjection")

    private companion object {
        private val description = """
            Implicit dependencies of composables should be made explicit.

            Acquiring a ViewModel should be done in composable default parameters, so that it is more testable and flexible.
        """.trimIndent()
    }
}
