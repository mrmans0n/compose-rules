// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core

import org.jetbrains.kotlin.com.intellij.psi.PsiElement

fun interface Emitter {
    fun report(element: PsiElement, errorMessage: String, canBeAutoCorrected: Boolean)
}

fun Emitter.report(element: PsiElement, errorMessage: String) {
    report(element = element, errorMessage = errorMessage, canBeAutoCorrected = false)
}
