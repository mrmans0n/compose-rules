// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isComposable
import io.nlopez.rules.core.util.isInternal
import io.nlopez.rules.core.util.isPrivate
import io.nlopez.rules.core.util.isProtected
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class ComposeDefaultsVisibility : ComposeKtVisitor {

    override fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter) {

        val composables = file.findChildrenByClass<KtFunction>()
            .filter { it.isComposable }

        val composableNamesForDefaults = composables.mapNotNull { it.name }.map { it + "Defaults" }.toSet()

        // Default holders should be the ones named ${composableName}Defaults and defined in the same .kt file as them.
        val defaultObjects = file.findChildrenByClass<KtClassOrObject>()
            .filter { it.name in composableNamesForDefaults }

        if (defaultObjects.count() == 0) return


        // First we have to make sure the defaults class is used inside of the composable (e.g. in the params
        // in some way).

        // Now we need to cross-reference our default objects with the composables they match to check if the
        // visibility matches the most visible composable
        
        // If we find a "defaults" object with less visibility than its composable, we report it
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

        fun createMessage(composable: KtFunction, defaultObject: KtClassOrObject): String = """
            @Composable `Defaults` objects should match visibility of the composables they serve.

            ${defaultObject.name} is ${defaultObject.visibilityString} but it should be ${composable.visibilityString}.

            See https://mrmans0n.github.io/compose-rules/rules/#TODO for more information.
        """.trimIndent()
    }
}
}
