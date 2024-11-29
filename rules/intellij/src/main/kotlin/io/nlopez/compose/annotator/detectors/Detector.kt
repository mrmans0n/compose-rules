// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator.detectors

import io.nlopez.compose.annotator.errors.ComposableError
import org.jetbrains.kotlin.psi.KtFunction

fun interface Detector {
    operator fun invoke(composable: KtFunction): List<ComposableError>
}
