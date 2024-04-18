// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

val KtAnnotated.isComposable: Boolean
    get() = annotationEntries.any { it.calleeExpression?.text == "Composable" }

fun KtElement.isSuppressed(suppression: String): Boolean = parentsWithSelf.filterIsInstance<KtAnnotated>()
    .flatMap { it.annotationEntries }
    .filter { it.calleeExpression?.text == "Suppress" }
    .flatMap { it.valueArguments }
    .map { it.getArgumentExpression() }
    .filterIsInstance<KtStringTemplateExpression>()
    .flatMap { it.entries.asSequence() }
    .filterIsInstance<KtLiteralStringTemplateEntry>()
    .any { it.text == suppression }
