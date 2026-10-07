// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.KtlintRule
import io.nlopez.compose.rules.ModifierShouldBeForwardedToEveryBranch

class ModifierShouldBeForwardedToEveryBranchCheck :
    KtlintRule(
        id = "compose:modifier-should-be-forwarded-to-every-branch",
        editorConfigProperties = setOf(customModifiers),
    ),
    ComposeKtVisitor by ModifierShouldBeForwardedToEveryBranch()
