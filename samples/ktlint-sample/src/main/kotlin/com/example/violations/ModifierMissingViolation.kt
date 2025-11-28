// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Violation: Missing Modifier parameter (ModifierMissing)
// Public composables should have a Modifier parameter
@Composable
fun ViolationMissingModifier() {
    Text("Hello World")
}

