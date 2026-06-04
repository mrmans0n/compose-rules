// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

// Violation: can be marked @ReadOnlyComposable (MissingReadOnlyComposable)
@Composable
fun MissingReadOnlyComposableViolation(): Int = readOnlyValue()

@ReadOnlyComposable
@Composable
private fun readOnlyValue(): Int = 42
