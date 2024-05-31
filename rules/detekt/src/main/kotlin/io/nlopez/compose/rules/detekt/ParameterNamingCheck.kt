// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ParameterNaming

class ParameterNamingCheck(config: Config) :
    DetektRule(config, description),
    ComposeKtVisitor by ParameterNaming() {

    override val ruleId = Id("ParameterNaming")

    private companion object {
        private val description = """
        Lambda parameters in a composable function should be in present tense, not past tense.

        Examples: `onClick` and not `onClicked`, `onTextChange` and not `onTextChanged`, etc.
        """.trimIndent()
    }
}
