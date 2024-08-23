// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.composableLambdaTypes
import io.nlopez.compose.core.util.isComposableLambda
import io.nlopez.compose.core.util.lambdaTypes
import org.jetbrains.kotlin.psi.KtFunction

class ContentTrailingLambda : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        val lambdaTypes = function.containingKtFile.lambdaTypes(config)
        val composableLambdaTypes = function.containingKtFile.composableLambdaTypes(config)

        val candidate = function.valueParameters
            .filter { it.name == "content" }
            .singleOrNull { parameter ->
                parameter.typeReference?.isComposableLambda(lambdaTypes, composableLambdaTypes) == true
            }

        if (candidate != null && candidate != function.valueParameters.last()) {
            emitter.report(candidate, ContentShouldBeTrailingLambda)
        }
    }

    companion object {
        val ContentShouldBeTrailingLambda = """
            A @Composable `content` parameter should be moved to be the trailing lambda in a composable function.

            See https://mrmans0n.github.io/compose-rules/rules/#slots-for-main-content-should-be-the-trailing-lambda for more information.
        """.trimIndent()
    }
}
