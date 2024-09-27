// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ContentSlotReused
import io.nlopez.compose.rules.KtlintRule

class ContentSlotReusedCheck :
    KtlintRule(
        id = "compose:content-slot-reused",
        editorConfigProperties = setOf(treatAsLambda, treatAsComposableLambda),
    ),
    ComposeKtVisitor by ContentSlotReused()
