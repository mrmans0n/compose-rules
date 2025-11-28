// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Violation: Multiple content emitters (MultipleEmitters)
// A composable should emit content only once
@Composable
fun ViolationMultipleEmitters(modifier: Modifier = Modifier) {
    Text("First emitter")
    Text("Second emitter")
    Column(modifier = modifier) {
        Text("Third emitter")
    }
}

