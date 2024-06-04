// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import org.jetbrains.kotlin.psi.KtFunction

class MutableStateParameter : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        function.valueParameters
            .filter { it.typeReference?.text?.matches(MutableStateRegex) == true }
            .forEach { emitter.report(it, MutableStateParameterInCompose) }
    }

    companion object {
        private val MutableStateRegex = "MutableState<.*>\\??".toRegex()

        val MutableStateParameterInCompose = """
            MutableState shouldn't be used as a parameter in a @Composable function, as it promotes joint ownership over a state between a component and its user.

            If possible, consider making the component stateless and concede the state change to the caller. If mutation of the parent’s owned property is required in the component, consider creating a ComponentState class with the domain specific meaningful field that is backed by mutableStateOf().

            See https://mrmans0n.github.io/compose-rules/rules/#do-not-use-mutablestate-as-a-parameter for more information.
        """.trimIndent()
    }
}
