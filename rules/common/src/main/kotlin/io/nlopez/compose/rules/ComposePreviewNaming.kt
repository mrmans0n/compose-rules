// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.isPreview
import io.nlopez.rules.core.util.isPreviewAnnotation
import org.jetbrains.kotlin.psi.KtClass

class ComposePreviewNaming : ComposeKtVisitor {
    override fun visitClass(clazz: KtClass, autoCorrect: Boolean, emitter: Emitter) {
        if (!clazz.isAnnotation()) return
        if (!clazz.isPreview) return

        // We know here that we are in an annotation that either has a @Preview or other preview annotations
        val count = clazz.annotationEntries.count { it.isPreviewAnnotation }
        val name = clazz.nameAsSafeName.asString()
        if (count == 1 && !name.endsWith("Preview")) {
            emitter.report(clazz, createMessage(count, "Preview"))
        } else if (count > 1 && !name.endsWith("Previews")) {
            emitter.report(clazz, createMessage(count, "Previews"))
        }
    }

    companion object {
        fun createMessage(count: Int, suggestedSuffix: String): String = """
            Preview annotations with $count preview annotations should end with the `$suggestedSuffix` suffix.

            See https://twitter.github.io/compose-rules/rules/#naming-multipreview-annotations-properly for more information.
        """.trimIndent()
    }
}
