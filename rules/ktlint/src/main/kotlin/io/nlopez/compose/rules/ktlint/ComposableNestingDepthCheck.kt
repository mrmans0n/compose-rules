// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ComposableNestingDepth
import io.nlopez.compose.rules.KtlintRule

class ComposableNestingDepthCheck :
    KtlintRule(
        id = "compose:composable-nesting-depth-check",
        editorConfigProperties = setOf(
            composableNestingDepthThreshold,
            contentEmittersProperty,
            contentEmittersDenylist,
        ),
    ),
    ComposeKtVisitor by ComposableNestingDepth()
