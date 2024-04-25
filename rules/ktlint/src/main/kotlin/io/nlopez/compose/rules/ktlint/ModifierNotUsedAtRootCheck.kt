// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ModifierNotUsedAtRoot
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ModifierNotUsedAtRootCheck :
    KtlintRule(
        id = "compose:modifier-not-used-at-root",
        editorConfigProperties = setOf(contentEmittersProperty, customModifiers, contentEmittersDenylist),
    ),
    ComposeKtVisitor by ModifierNotUsedAtRoot()
