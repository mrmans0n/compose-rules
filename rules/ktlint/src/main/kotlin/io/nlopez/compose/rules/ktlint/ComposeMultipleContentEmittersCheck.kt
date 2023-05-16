// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ComposeMultipleContentEmitters
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ComposeMultipleContentEmittersCheck :
    KtlintRule(
        id = "compose:multiple-emitters-check",
        editorConfigProperties = setOf(contentEmittersProperty),
    ),
    ComposeKtVisitor by ComposeMultipleContentEmitters()
