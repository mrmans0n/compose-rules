// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import org.jetbrains.kotlin.psi.KtFunction

class StateParameter : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        function.valueParameters
            .filter { it.typeReference?.text?.matches(StateRegex) == true }
            .forEach { emitter.report(it, StateParameterInCompose) }
    }

    companion object {
        private val StateRegex = "(State<.*>|(Int|Float|Double|Long)State)\\??".toRegex()

        val StateParameterInCompose = """
            State shouldn't be used as a parameter in a @Composable function, as it encourages components that are not fully stateless.
            Instead, pass the snapshot state value and provide event callbacks for changes.
            See https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-component-api-guidelines.md#statet-as-a-parameter for more information.
        """.trimIndent()
    }
}
