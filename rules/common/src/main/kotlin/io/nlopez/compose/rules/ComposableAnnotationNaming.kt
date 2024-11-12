// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass

class ComposableAnnotationNaming : ComposeKtVisitor {
    override fun visitClass(clazz: KtClass, emitter: Emitter, config: ComposeKtConfig) {
        if (!clazz.isAnnotation()) return
        if (!clazz.isComposableTargetMarkerAnnotation) return

        val name = clazz.nameAsSafeName.asString()
        if (!name.endsWith("Composable")) {
            emitter.report(clazz, ComposableAnnotationDoesNotEndWithComposable)
        }
    }

    private val KtAnnotated.isComposableTargetMarkerAnnotation: Boolean
        get() = annotationEntries.any {
            it.calleeExpression?.text?.contains("ComposableTargetMarker") == true
        }

    companion object {
        val ComposableAnnotationDoesNotEndWithComposable = """
            Composable annotations (e.g. tagged with `@ComposableTargetMarker`) should have the `Composable` suffix.
            See https://mrmans0n.github.io/compose-rules/rules/#naming-composable-annotations-properly for more information.
        """.trimIndent()
    }
}
