// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.composableLambdaTypes
import io.nlopez.compose.core.util.isLambda
import org.jetbrains.kotlin.psi.KtFunction

class ContentTrailingLambda : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) = with(config) {
        val lambdaTypes = function.containingKtFile.composableLambdaTypes

        val candidate = function.valueParameters
            .filter { it.name == "content" }
            .singleOrNull { it.typeReference?.isLambda(lambdaTypes) == true }

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
