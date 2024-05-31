// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.RememberStateMissing

class RememberStateMissingCheck(config: Config) :
    DetektRule(config, description),
    ComposeKtVisitor by RememberStateMissing() {

    override val ruleId = Id("RememberMissing")

    private companion object {
        private val description = """
            Using mutableStateOf/derivedStateOf in a @Composable function without it being inside of a remember function.
            If you don't remember the state instance, a new state instance will be created when the function is recomposed.
        """.trimIndent()
    }
}
