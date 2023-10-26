// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ComposeRememberContentMissing
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ComposeRememberContentMissingCheck :
    KtlintRule("compose:remember-content-missing-check"),
    ComposeKtVisitor by ComposeRememberContentMissing()
