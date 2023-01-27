// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ComposeCompositionLocalAllowlistCheck :
    KtlintRule("compose:compositionlocal-allowlist"),
    ComposeKtVisitor by io.nlopez.compose.rules.ComposeCompositionLocalAllowlist()
