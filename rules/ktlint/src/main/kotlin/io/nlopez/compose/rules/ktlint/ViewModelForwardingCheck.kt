// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ViewModelForwarding
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.KtlintRule

class ViewModelForwardingCheck :
    KtlintRule(
        id = "compose:vm-forwarding-check",
        editorConfigProperties = setOf(allowedStateHolderNames),
    ),
    ComposeKtVisitor by ViewModelForwarding()
