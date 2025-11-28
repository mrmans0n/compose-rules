// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Violation: Past-tense lambda parameter (ParameterNaming)
// Lambda parameters should use present tense (onClick, not onClicked)
@Composable
fun ViolationPastTenseLambda(
    onClicked: () -> Unit, // Should be: onClick
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClicked, modifier = modifier) {
        Text("Click me")
    }
}

