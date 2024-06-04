// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun interface Emitter {
    fun report(element: PsiElement, errorMessage: String, canBeAutoCorrected: Boolean): Decision
}

fun Emitter.report(element: PsiElement, errorMessage: String) =
    report(element = element, errorMessage = errorMessage, canBeAutoCorrected = false)

enum class Decision {
    Fix,
    Ignore,
}

@OptIn(ExperimentalContracts::class)
fun Decision.ifFix(block: () -> Unit) {
    contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }
    if (this == Decision.Fix) block()
}
