// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiFile
import io.nlopez.compose.annotator.detectors.LambdaToMethodReferenceDetector
import io.nlopez.compose.annotator.detectors.ParameterOrderDetector
import io.nlopez.compose.annotator.errors.ComposableError
import io.nlopez.compose.annotator.findAllChildrenByType
import io.nlopez.compose.annotator.isComposable
import io.nlopez.compose.annotator.runIf
import org.jetbrains.kotlin.psi.KtFunction

class ComposeAnnotator : ExternalAnnotator<List<KtFunction>, List<ComposableError>>() {

    override fun collectInformation(file: PsiFile): List<KtFunction> = runReadAction {
        file.findAllChildrenByType<KtFunction>()
            .filter { it.isComposable }
            .toList()
    }

    override fun doAnnotate(collectedInfo: List<KtFunction>): List<ComposableError> = runReadAction {
        buildList<ComposableError> {
            // Check for parameter order
            addAll(collectedInfo.flatMap(ParameterOrderDetector::invoke))

            // Check for lambdas that can be substituted by references
            addAll(collectedInfo.flatMap(LambdaToMethodReferenceDetector::invoke))
        }
    }

    override fun apply(file: PsiFile, annotationResult: List<ComposableError>, holder: AnnotationHolder) {
        annotationResult
            .filterNot { it.isSuppressed }
            .map { issue ->
                holder.newAnnotation(issue.severity, issue.message)
                    .range(issue.markerTag)
                    .runIf(issue.hasFix) { newFix(issue.fix()).registerFix() }
                    .newFix(issue.createFileSuppression()).registerFix()
                    .newFix(issue.createStatementSuppression()).registerFix()
            }
            .forEach { annotationBuilder -> annotationBuilder.create() }
    }
}
