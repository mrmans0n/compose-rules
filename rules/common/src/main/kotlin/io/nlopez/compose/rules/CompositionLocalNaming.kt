// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.declaresCompositionLocal
import io.nlopez.compose.core.util.findAllChildrenByClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class CompositionLocalNaming : ComposeKtVisitor {

    override fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {
        val compositionLocals = file.findAllChildrenByClass<KtProperty>()
            .filter { it.declaresCompositionLocal }

        if (compositionLocals.none()) return

        val notAllowed = compositionLocals.filterNot { it.nameIdentifier?.text?.startsWith("Local") == true }

        for (compositionLocal in notAllowed) {
            emitter.report(compositionLocal, CompositionLocalNeedsLocalPrefix)
        }
    }

    companion object {
        val CompositionLocalNeedsLocalPrefix = """
            CompositionLocals should be named using the `Local` prefix as an adjective, followed by a descriptive noun.
            See https://mrmans0n.github.io/compose-rules/rules/#naming-compositionlocals-properly for more information.
        """.trimIndent()
    }
}
