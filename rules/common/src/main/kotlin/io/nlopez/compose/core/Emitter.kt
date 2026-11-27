// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core

import com.intellij.psi.PsiElement

fun interface Emitter {
    fun report(element: PsiElement, errorMessage: String, autoCorrect: (() -> Unit)?)
}

fun Emitter.report(element: PsiElement, errorMessage: String) =
    report(element = element, errorMessage = errorMessage, autoCorrect = null)
