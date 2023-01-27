// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry

val KtAnnotated.isPreview: Boolean
    get() = annotationEntries.any { it.isPreviewAnnotation }

val KtAnnotationEntry.isPreviewAnnotation: Boolean
    get() = calleeExpression?.text?.let { PreviewNameRegex.matches(it) } == true

val KtAnnotated.isPreviewParameter: Boolean
    get() = annotationEntries.any { it.calleeExpression?.text == "PreviewParameter" }

val PreviewNameRegex by lazy {
    Regex(".*Preview[s]*$")
}
