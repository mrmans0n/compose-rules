// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ModifierNaming
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ModifierNamingCheck :
    KtlintRule(
        id = "compose:modifier-naming",
        editorConfigProperties = setOf(customModifiers),
    ),
    ComposeKtVisitor by ModifierNaming()
