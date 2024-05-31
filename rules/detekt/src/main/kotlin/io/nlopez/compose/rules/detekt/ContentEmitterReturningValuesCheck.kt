// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ContentEmitterReturningValues
import io.nlopez.compose.rules.DetektRule

class ContentEmitterReturningValuesCheck(config: Config) :
    DetektRule(config, ContentEmitterReturningValues.ContentEmitterReturningValuesToo),
    ComposeKtVisitor by ContentEmitterReturningValues() {

    override val ruleId = Id("ContentEmitterReturningValues")
}
