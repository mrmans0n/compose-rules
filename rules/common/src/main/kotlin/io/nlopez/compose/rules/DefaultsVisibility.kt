// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.findAllChildrenByClass
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.isInternal
import io.nlopez.compose.core.util.isPrivate
import io.nlopez.compose.core.util.isProtected
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class DefaultsVisibility : ComposeKtVisitor {

    override fun visitClassOrObject(clazz: KtClassOrObject, emitter: Emitter, config: ComposeKtConfig) {
        val defaultObjectName = clazz.name ?: return
        if (!defaultObjectName.endsWith("Defaults")) return

        val composableName = defaultObjectName.removeSuffix("Defaults")
        if (composableName.isEmpty()) return

        val mostVisibleComposable = clazz.containingKtFile
            .findAllChildrenByClass<KtFunction>()
            .filter { it.isComposable }
            .filter { it.name == composableName }
            .filter { composable ->
                val hasReferenceInParameters = composable.valueParameters
                    .mapNotNull { it.defaultValue }
                    .flatMap { it.findAllChildrenByClass<KtReferenceExpression>() }
                    .any { it.text == defaultObjectName }

                if (hasReferenceInParameters) return@filter true

                val body = composable.bodyBlockExpression ?: return@filter false
                body.findAllChildrenByClass<KtReferenceExpression>()
                    .any { it.text == defaultObjectName }
            }
            .maxByOrNull { it.visibilityInt }
            ?: return

        if (clazz.visibilityInt < mostVisibleComposable.visibilityInt) {
            emitter.report(
                element = clazz,
                errorMessage = createMessage(
                    composableVisibility = mostVisibleComposable.visibilityString,
                    defaultObjectName = defaultObjectName,
                    defaultObjectVisibility = clazz.visibilityString,
                ),
            )
        }
    }

    companion object {

        private val KtModifierListOwner.visibilityString: String
            get() = when {
                isPublic -> "public"
                isProtected -> "protected"
                isInternal -> "internal"
                isPrivate -> "private"
                else -> "not supported"
            }

        private val KtModifierListOwner.visibilityInt: Int
            get() = when {
                isPublic -> 4
                isInternal -> 3
                isProtected -> 2
                isPrivate -> 1
                else -> 0
            }

        fun createMessage(composableVisibility: String, defaultObjectName: String, defaultObjectVisibility: String) =
            """
            `Defaults` objects should match visibility of the composables they serve. `$defaultObjectName` is $defaultObjectVisibility but it should be $composableVisibility.
            See https://mrmans0n.github.io/compose-rules/rules/#componentdefaults-object-should-match-the-composable-visibility for more information.
            """.trimIndent()
    }
}
