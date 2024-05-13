// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.KtlintRule
import io.nlopez.compose.rules.MutableParameters

class MutableParametersCheck :
    KtlintRule("compose:mutable-params-check"),
    ComposeKtVisitor by MutableParameters()
