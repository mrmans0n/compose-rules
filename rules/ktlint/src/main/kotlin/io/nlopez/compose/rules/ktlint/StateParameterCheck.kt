// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.KtlintRule
import io.nlopez.compose.rules.StateParameter

class StateParameterCheck :
    KtlintRule("compose:state-param-check"),
    ComposeKtVisitor by StateParameter()
