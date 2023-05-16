// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ComposeContentEmitterReturningValues
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ComposeContentEmitterReturningValuesCheck :
    KtlintRule(
        id = "compose:content-emitter-returning-values-check",
        editorConfigProperties = setOf(contentEmittersProperty),
    ),
    ComposeKtVisitor by ComposeContentEmitterReturningValues()
