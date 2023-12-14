// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ModifierMissing
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ModifierMissingCheck :
    KtlintRule(
        id = "compose:modifier-missing-check",
        editorConfigProperties = setOf(checkModifiersForVisibility, contentEmittersProperty, customModifiers),
    ),
    ComposeKtVisitor by ModifierMissing()
