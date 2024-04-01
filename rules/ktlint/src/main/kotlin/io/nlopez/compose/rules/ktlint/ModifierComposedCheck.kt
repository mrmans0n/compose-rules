// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ModifierComposed
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ModifierComposedCheck :
    KtlintRule(
        id = "compose:modifier-composed-check",
        editorConfigProperties = setOf(customModifiers),
    ),
    ComposeKtVisitor by ModifierComposed()
