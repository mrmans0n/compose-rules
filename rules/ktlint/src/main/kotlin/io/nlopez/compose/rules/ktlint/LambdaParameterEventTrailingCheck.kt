// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.KtlintRule
import io.nlopez.compose.rules.LambdaParameterEventTrailing

class LambdaParameterEventTrailingCheck :
    KtlintRule(
        id = "compose:lambda-param-event-trailing",
        editorConfigProperties = setOf(contentEmittersProperty, contentEmittersDenylist),
    ),
    ComposeKtVisitor by LambdaParameterEventTrailing()
