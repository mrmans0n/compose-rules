// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.composableLambdaTypes
import io.nlopez.compose.core.util.isLambda
import org.jetbrains.kotlin.psi.KtFunction

class ContentSlotLifecycle : ComposeKtVisitor {
    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) = with(config) {
        val lambdaTypes = function.containingKtFile.composableLambdaTypes
        val slots = function.valueParameters
            .singleOrNull { it.typeReference?.isLambda(lambdaTypes) == true }
    }
}
