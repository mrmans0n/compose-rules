// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ModifierClickableOrder
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ModifierClickableOrderCheck :
    KtlintRule(
        id = "compose:modifier-clickable-order",
        editorConfigProperties = setOf(customModifiers),
    ),
    ComposeKtVisitor by ModifierClickableOrder()
