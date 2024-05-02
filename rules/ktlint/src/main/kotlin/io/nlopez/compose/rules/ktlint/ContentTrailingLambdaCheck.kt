// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ContentTrailingLambda
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ContentTrailingLambdaCheck :
    KtlintRule(
        id = "compose:content-trailing-lambda",
        editorConfigProperties = setOf(treatAsComposableLambda),
    ),
    ComposeKtVisitor by ContentTrailingLambda()
