// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.LambdaParameterInRestartableEffect
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class LambdaParameterInRestartableEffectCheck :
    KtlintRule(
        id = "compose:lambda-param-in-effect",
        editorConfigProperties = setOf(treatAsLambda),
    ),
    ComposeKtVisitor by LambdaParameterInRestartableEffect()
