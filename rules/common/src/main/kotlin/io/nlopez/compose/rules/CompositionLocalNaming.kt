// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.declaresCompositionLocal
import io.nlopez.rules.core.util.findChildrenByClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class CompositionLocalNaming : ComposeKtVisitor {

    override fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {
        val compositionLocals = file.findChildrenByClass<KtProperty>()
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
