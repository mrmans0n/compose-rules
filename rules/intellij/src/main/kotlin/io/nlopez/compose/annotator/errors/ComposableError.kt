// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator.errors

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement

sealed class ComposableError(val source: KtElement) {
    open val markerTag: PsiElement = source
    abstract val severity: HighlightSeverity
    abstract val message: String
    open val hasFix: Boolean = false
    open fun fix(): IntentionAction = error("No fix for you")
}
