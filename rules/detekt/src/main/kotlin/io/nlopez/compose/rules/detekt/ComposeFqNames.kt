// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.name.FqName

internal object ComposeFqNames {
    internal var runtime: FqName = FqName("androidx.compose.runtime")
    internal var runtimeSaveable: FqName = FqName("androidx.compose.runtime.saveable")
    internal var runtimeRetain: FqName = FqName("androidx.compose.runtime.retain")

    val Composable: FqName
        get() = runtime.child("Composable")
    val Remember: FqName
        get() = runtime.child("remember")
    val RememberUpdatedState: FqName
        get() = runtime.child("rememberUpdatedState")
    val RememberSaveable: FqName
        get() = runtimeSaveable.child("rememberSaveable")
    val Retain: FqName
        get() = runtimeRetain.child("retain")

    private fun FqName.child(name: String): FqName = FqName("${asString()}.$name")
}
