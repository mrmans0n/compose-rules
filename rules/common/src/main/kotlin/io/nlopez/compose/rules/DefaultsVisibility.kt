// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.isInternal
import io.nlopez.compose.core.util.isPrivate
import io.nlopez.compose.core.util.isProtected
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class DefaultsVisibility : ComposeKtVisitor {

    override fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {
        val composables = file.findChildrenByClass<KtFunction>()
            .filter { it.isComposable }

        val composableNamesForDefaults = composables.mapNotNull { it.name }.map { it + "Defaults" }.toSet()

        // Default holders should be the ones named ${composableName}Defaults and defined in the same .kt file as them,
        // as they should be co-located. Maybe a possible future rule would be to check for co-location of these.
        val defaultObjects = file.findChildrenByClass<KtClassOrObject>()
            .filter { it.name in composableNamesForDefaults }

        if (defaultObjects.count() == 0) return

        // We want to obtain the pairing of the default objects to their most visible composable counterparts.
        // Hold on to your butts.
        val defaultToMostVisibleComposable = defaultObjects.map { defaultObject ->
            // Find the matching composables to the default object
            val mostVisible = composables.filter { it.name + "Defaults" == defaultObject.name }
                .filter { composable ->
                    // Now we need to check whether anything from the default object is used either in the params
                    // or in the code of the composable itself. This should be enough for most cases.

                    // Check parameter defaults first
                    val hasReferenceInParameters = composable.valueParameters
                        .mapNotNull { it.defaultValue }
                        .flatMap { it.findChildrenByClass<KtReferenceExpression>() }
                        .any { it.text == defaultObject.name }

                    if (hasReferenceInParameters) return@filter true

                    // If none found, check the code then.
                    val body = composable.bodyBlockExpression ?: return@filter false
                    return@filter body.findChildrenByClass<KtReferenceExpression>()
                        .any { it.text == defaultObject.name }
                }
                // Now we want to obtain just the most visible visibility in case there are more than one hit
                .maxByOrNull { it.visibilityInt }

            defaultObject to mostVisible
        }

        // If we find a "defaults" object with less visibility than its composable, we report it
        for ((defaultObject, composable) in defaultToMostVisibleComposable) {
            if (composable != null && defaultObject.visibilityInt < composable.visibilityInt) {
                emitter.report(
                    element = defaultObject,
                    errorMessage = createMessage(
                        composableVisibility = composable.visibilityString,
                        defaultObjectName = defaultObject.name!!,
                        defaultObjectVisibility = defaultObject.visibilityString,
                    ),
                )
            }
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
