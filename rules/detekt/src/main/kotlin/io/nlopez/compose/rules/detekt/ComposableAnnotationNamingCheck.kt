// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ComposableAnnotationNaming
import io.nlopez.compose.rules.DetektRule

class ComposableAnnotationNamingCheck(config: Config) :
    DetektRule(config, ComposableAnnotationNaming.ComposableAnnotationDoesNotEndWithComposable),
    ComposeKtVisitor by ComposableAnnotationNaming() {

    override val ruleId = Id("ComposableAnnotationNaming")
}
