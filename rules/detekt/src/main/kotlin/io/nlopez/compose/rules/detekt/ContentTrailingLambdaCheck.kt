// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ContentTrailingLambda
import io.nlopez.compose.rules.DetektRule

class ContentTrailingLambdaCheck(config: Config) :
    DetektRule(config, ContentTrailingLambda.ContentShouldBeTrailingLambda),
    ComposeKtVisitor by ContentTrailingLambda() {

    override val ruleId = Id("ContentTrailingLambda")
}
