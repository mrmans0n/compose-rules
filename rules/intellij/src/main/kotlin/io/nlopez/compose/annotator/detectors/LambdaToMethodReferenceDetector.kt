// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator.detectors

import io.nlopez.compose.annotator.errors.ComposableError
import io.nlopez.compose.annotator.errors.LambdaToMethodReferenceError
import io.nlopez.compose.annotator.findAllChildrenByType
import io.nlopez.compose.annotator.isComposable
import io.nlopez.compose.annotator.isComposableDestination
import org.jetbrains.kotlin.idea.intentions.ConvertLambdaToReferenceIntention
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression

object LambdaToMethodReferenceDetector : Detector {

    // This is the intention IJ uses to detect and convert lambdas to method references:
    // https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/intentions/ConvertLambdaToReferenceIntention.kt
    private val intention by lazy(LazyThreadSafetyMode.NONE) { ConvertLambdaToReferenceIntention() }
    override fun invoke(composable: KtFunction): List<ComposableError> {
        if (!composable.hasBody()) return emptyList()

        // Find all call expressions (to, likely composables, we don't know at this point)
        return composable.findAllChildrenByType<KtCallExpression>()
            // Find all lambda arguments in @Composable invocation targets
            .flatMap { callExpression ->
                val arguments = callExpression.valueArguments

                // KtValueArgument => KtLambdaExpression - non-trailing lambdas
                val lambdas = arguments.mapNotNull { it.getArgumentExpression() }
                    .filterIsInstance<KtLambdaExpression>()

                // KtLambdaArgument => trailing lambdas
                val trailingLambdas = arguments.filterIsInstance<KtLambdaArgument>()
                    // Check if the destination is a @Composable using IntelliJ's brains
                    .filter { callExpression.isComposableDestination }
                    // Check if the lambda itself is not a composable (e.g. `content: @Composable () -> Unit`)
                    .filterNot { it.isComposable }
                    .mapNotNull { lambdaArgument -> lambdaArgument.getLambdaExpression() }

                lambdas + trailingLambdas
            }
            // Detect if it can be refactored to be a method reference
            .filter { intention.isApplicableTo(it) }
            .map { LambdaToMethodReferenceError(it) }
            .toList()
    }
}
