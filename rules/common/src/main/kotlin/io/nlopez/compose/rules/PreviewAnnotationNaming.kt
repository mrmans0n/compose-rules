// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.isPreview
import org.jetbrains.kotlin.psi.KtClass

class PreviewAnnotationNaming : ComposeKtVisitor {
    override fun visitClass(clazz: KtClass, emitter: Emitter, config: ComposeKtConfig) {
        if (!clazz.isAnnotation()) return
        if (!clazz.isPreview) return

        val name = clazz.nameAsSafeName.asString()
        if (!name.startsWith("Preview")) {
            emitter.report(clazz, PreviewAnnotationDoesNotStartWithPreview)
        }
    }

    companion object {
        val PreviewAnnotationDoesNotStartWithPreview = """
            MultiPreview annotations should start with `Preview` as prefix.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-multipreview-annotations-properly for more information.
        """.trimIndent()
    }
}
