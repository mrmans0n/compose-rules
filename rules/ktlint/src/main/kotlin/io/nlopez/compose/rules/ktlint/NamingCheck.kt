// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.KtlintRule
import io.nlopez.compose.rules.Naming

class NamingCheck :
    KtlintRule(
        id = "compose:naming-check",
        editorConfigProperties = setOf(allowedComposeNamingNames),
    ),
    ComposeKtVisitor by Naming()
