// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.ComposeUnstableCollections
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.ktlint.TwitterKtlintRule

class ComposeUnstableCollectionsCheck :
    TwitterKtlintRule("compose:unstable-collections"),
    ComposeKtVisitor by ComposeUnstableCollections()
