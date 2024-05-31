// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.PreviewAnnotationNaming

class PreviewAnnotationNamingCheck(config: Config) :
    DetektRule(config, "Multipreview annotations should begin with the `Preview` suffix"),
    ComposeKtVisitor by PreviewAnnotationNaming() {

    override val ruleId = Id("PreviewAnnotationNaming")
}
