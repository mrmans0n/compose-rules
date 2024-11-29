// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator

import com.intellij.codeInsight.intention.IntentionAction
import io.nlopez.compose.annotator.errors.ComposableError
import org.jetbrains.kotlin.idea.inspections.suppress.AnnotationHostKind
import org.jetbrains.kotlin.idea.inspections.suppress.KotlinSuppressIntentionAction
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

/**
 * Traverse up the psi tree and try to find a @Suppress("ErrorName"). We will use the name of the class
 * that define the error. It also works for files `@file:Suppress("ErrorName")`.
 */
val ComposableError.isSuppressed: Boolean
    get() = source.parentsWithSelf
        // Find all items that can have an annotation
        .filterIsInstance<KtAnnotated>()
        .flatMap { it.annotationEntries }
        // Find any annotation named "Suppress"
        .filter { it.shortName?.asString() == "Suppress" }
        .flatMap { it.valueArguments }
        // Find the string inside of the annotation
        .mapNotNull { it.getArgumentExpression() }
        .filterIsInstance<KtStringTemplateExpression>()
        .any { it.text == "\"$ruleName\"" }

fun ComposableError.createStatementSuppression(): IntentionAction {
    val element = source.parentsWithSelf.filterIsInstance<KtExpression>().first()
    return KotlinSuppressIntentionAction(
        suppressAt = element,
        suppressionKey = ruleName,
        AnnotationHostKind(
            kind = "statement",
            name = element.name ?: element.text,
            newLineNeeded = true,
        ),
    )
}

fun ComposableError.createFileSuppression(): IntentionAction = KotlinSuppressIntentionAction(
    suppressAt = source.containingFile as KtFile,
    suppressionKey = ruleName,
    AnnotationHostKind(
        kind = "file",
        name = source.containingFile.name,
        newLineNeeded = true,
    ),
)

private val ComposableError.ruleName: String
    get() = this::class.simpleName!!.removeSuffix("Error")
