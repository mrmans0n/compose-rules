// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.runtime.Composable

// Violation: Uppercase composable returning value (ComposableNaming)
// Composables that return values should be lowercase
@Composable
fun ComputedValue(): Int {
    val result = 42
    return result
}

