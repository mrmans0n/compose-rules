// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

// Violation: Missing remember for mutableStateOf (RememberStateMissing)
@Composable
fun ViolationMissingRemember(modifier: Modifier = Modifier) {
    val count = mutableStateOf(0) // Should be: remember { mutableStateOf(0) }
    Button(onClick = { count.value++ }) {
        Text("Count: ${count.value}")
    }
}

