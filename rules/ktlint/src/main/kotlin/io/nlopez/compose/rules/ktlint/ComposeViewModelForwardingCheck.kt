// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ComposeViewModelForwarding
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ComposeViewModelForwardingCheck :
    KtlintRule(
        id = "compose:vm-forwarding-check",
        editorConfigProperties = setOf(allowedStateHolderNames),
    ),
    ComposeKtVisitor by ComposeViewModelForwarding()
