// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

// Violation: rememberUpdatedState read eagerly in remember (StaleRememberUpdatedStateInRemember)
@Composable
fun StaleRememberUpdatedStateViolation(onDismiss: () -> Unit) {
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val dialogCallbacks = remember {
        DialogCallbacks(onDismiss = latestOnDismiss)
    }
    dialogCallbacks.hashCode()
}

private class DialogCallbacks(val onDismiss: () -> Unit)
