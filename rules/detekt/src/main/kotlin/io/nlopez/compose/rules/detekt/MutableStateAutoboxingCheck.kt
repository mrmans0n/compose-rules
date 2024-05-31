// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.MutableStateAutoboxing

class MutableStateAutoboxingCheck(config: Config) :
    DetektRule(config, DESCRIPTION),
    ComposeKtVisitor by MutableStateAutoboxing() {

    override val ruleId = Id("MutableStateAutoboxing")

    private companion object {
        private const val DESCRIPTION = "Using mutableInt/Long/Double/FloatStateOf is recommended over " +
            "mutableStateOf<X> for Int/Long/Double/Float, as it uses the primitives directly which is more performant."
    }
}
