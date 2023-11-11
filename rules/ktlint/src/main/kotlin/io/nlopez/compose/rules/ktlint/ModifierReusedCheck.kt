// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ModifierReused
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ModifierReusedCheck :
    KtlintRule(
        id = "compose:modifier-reused-check",
        editorConfigProperties = setOf(contentEmittersProperty),
    ),
    ComposeKtVisitor by ModifierReused()
