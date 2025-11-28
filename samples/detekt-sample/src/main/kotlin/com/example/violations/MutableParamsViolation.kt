// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package com.example.violations

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Violation: Mutable parameter (MutableParams)
// Using ArrayList which is mutable
@Composable
fun ViolationMutableParameter(items: ArrayList<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        items.forEach { item ->
            Text(item)
        }
    }
}

