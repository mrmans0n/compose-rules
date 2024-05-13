// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ComposableAnnotationNaming
import io.nlopez.compose.rules.KtlintRule

class ComposableAnnotationNamingCheck :
    KtlintRule("compose:composable-annotation-naming"),
    ComposeKtVisitor by ComposableAnnotationNaming()
