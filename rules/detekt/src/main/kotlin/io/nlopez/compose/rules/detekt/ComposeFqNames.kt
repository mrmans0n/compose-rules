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
    val ReadOnlyComposable: FqName
        get() = runtime.child("ReadOnlyComposable")
    val NonRestartableComposable: FqName
        get() = runtime.child("NonRestartableComposable")
    val NonSkippableComposable: FqName
        get() = runtime.child("NonSkippableComposable")
    val ExplicitGroupsComposable: FqName
        get() = runtime.child("ExplicitGroupsComposable")
    val CompositionLocal: FqName
        get() = runtime.child("CompositionLocal")
    val State: FqName
        get() = runtime.child("State")
    val MutableState: FqName
        get() = runtime.child("MutableState")
    val IntState: FqName
        get() = runtime.child("IntState")
    val MutableIntState: FqName
        get() = runtime.child("MutableIntState")
    val LongState: FqName
        get() = runtime.child("LongState")
    val MutableLongState: FqName
        get() = runtime.child("MutableLongState")
    val FloatState: FqName
        get() = runtime.child("FloatState")
    val MutableFloatState: FqName
        get() = runtime.child("MutableFloatState")
    val DoubleState: FqName
        get() = runtime.child("DoubleState")
    val MutableDoubleState: FqName
        get() = runtime.child("MutableDoubleState")
    val Remember: FqName
        get() = runtime.child("remember")
    val RememberUpdatedState: FqName
        get() = runtime.child("rememberUpdatedState")
    val RememberSaveable: FqName
        get() = runtimeSaveable.child("rememberSaveable")
    val Retain: FqName
        get() = runtimeRetain.child("retain")
    val LaunchedEffect: FqName
        get() = runtime.child("LaunchedEffect")
    val SideEffect: FqName
        get() = runtime.child("SideEffect")

    private fun FqName.child(name: String): FqName = FqName("${asString()}.$name")
}
