// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.declaresCompositionLocal
import io.nlopez.compose.core.util.findChildrenByClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class CompositionLocalAllowlist : ComposeKtVisitor {

    override fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {
        val compositionLocals = file.findChildrenByClass<KtProperty>()
            .filter { it.declaresCompositionLocal }

        if (compositionLocals.none()) return

        val allowed = config.getSet("allowedCompositionLocals", emptySet())
        val notAllowed = compositionLocals.filterNot { allowed.contains(it.nameIdentifier?.text) }

        for (compositionLocal in notAllowed) {
            emitter.report(
                compositionLocal,
                CompositionLocalNotInAllowlist,
            )
        }
    }

    companion object {
        val CompositionLocalNotInAllowlist = """
            CompositionLocals are implicit dependencies and creating new ones should be avoided.

            See https://mrmans0n.github.io/compose-rules/rules/#compositionlocals for more information.
        """.trimIndent()
    }
}
