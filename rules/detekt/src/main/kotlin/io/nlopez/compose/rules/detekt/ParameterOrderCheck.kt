// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ParameterOrder

class ParameterOrderCheck(config: Config) :
    DetektRule(config, DESCRIPTION),
    ComposeKtVisitor by ParameterOrder() {

    override val ruleId = Id("ComposableParamOrder")

    private companion object {
        private const val DESCRIPTION = "Parameters in a composable function should be ordered following this " +
            "pattern: params without defaults, modifiers, params with defaults and optionally, " +
            "a trailing function that might not have a default param."
    }
}
