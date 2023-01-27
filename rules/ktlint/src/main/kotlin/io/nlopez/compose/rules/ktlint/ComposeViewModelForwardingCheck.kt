// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ComposeViewModelForwarding
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.TwitterKtlintRule

class ComposeViewModelForwardingCheck :
    TwitterKtlintRule("twitter-compose:vm-forwarding-check"),
    ComposeKtVisitor by ComposeViewModelForwarding()
