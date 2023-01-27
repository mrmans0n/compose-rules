// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig.Companion.config
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.declaresCompositionLocal
import io.nlopez.rules.core.util.findChildrenByClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class ComposeCompositionLocalAllowlist : ComposeKtVisitor {

    override fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter) {
        val compositionLocals = file.findChildrenByClass<KtProperty>()
            .filter { it.declaresCompositionLocal }

        if (compositionLocals.none()) return

        val allowed = file.config().getSet("allowedCompositionLocals", emptySet())
        val notAllowed = compositionLocals.filterNot { allowed.contains(it.nameIdentifier?.text) }

        for (compositionLocal in notAllowed) {
            emitter.report(
                compositionLocal,
                io.nlopez.compose.rules.ComposeCompositionLocalAllowlist.Companion.CompositionLocalNotInAllowlist,
            )
        }
    }

    companion object {
        val CompositionLocalNotInAllowlist = """
            CompositionLocals are implicit dependencies and creating new ones should be avoided.

            See https://twitter.github.io/compose-rules/rules/#compositionlocals for more information.
        """.trimIndent()
    }
}
