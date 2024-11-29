// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator.errors

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.HighlightSeverity
import org.jetbrains.kotlin.idea.intentions.ConvertLambdaToReferenceIntention
import org.jetbrains.kotlin.psi.KtLambdaExpression

data class LambdaToMethodReferenceError(val lambda: KtLambdaExpression) : ComposableError(lambda) {
    override val severity: HighlightSeverity = HighlightSeverity.WARNING

    override val message: String = """
        This lambda should be converted to a method reference.
        In Compose, method references are @Stable functional types and will remain equivalent between recompositions.
    """.trimIndent()
    override val hasFix: Boolean = true
    override fun fix(): IntentionAction =
        ConvertLambdaToReferenceIntention { "Convert lambda parameter in a @Composable to reference" }
}
