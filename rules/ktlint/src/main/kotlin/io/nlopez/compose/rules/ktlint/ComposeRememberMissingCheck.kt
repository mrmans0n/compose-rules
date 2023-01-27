// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ComposeRememberMissing
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.TwitterKtlintRule

class ComposeRememberMissingCheck :
    TwitterKtlintRule("twitter-compose:remember-missing-check"),
    ComposeKtVisitor by ComposeRememberMissing()
