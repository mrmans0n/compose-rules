// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.definedInInterface
import io.nlopez.compose.core.util.emitsContent
import io.nlopez.compose.core.util.isAnnotatedWith
import io.nlopez.compose.core.util.isInternal
import io.nlopez.compose.core.util.isModifierReceiver
import io.nlopez.compose.core.util.isOverride
import io.nlopez.compose.core.util.isPreview
import io.nlopez.compose.core.util.modifierParameter
import io.nlopez.compose.core.util.returnsValue
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class ModifierMissing : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        // We want to find all composable functions that:
        //  - emit content
        //  - are not overridden or part of an interface
        //  - are not a @Preview composable
        //  - are not Modifier factory functions
        if (
            function.returnsValue ||
            function.isOverride ||
            function.definedInInterface ||
            function.isPreview ||
            function.isModifierReceiver(config)
        ) {
            return
        }

        // Ignore functions annotated with the given set of names
        if (function.isAnnotatedWith(config.getSet("modifierMissingIgnoreAnnotated", emptySet()))) return

        // We want to check now the visibility to see whether it's allowed by the configuration
        // Possible values:
        // - only_public: will check for modifiers only on public composables
        // - public_and_internal: will check for public and internal composables
        // - all: will check all composables (public, internal, protected, private
        val shouldCheck = when (
            config.getString("checkModifiersForVisibility", "only_public")
        ) {
            "only_public" -> function.isPublic
            "public_and_internal" -> function.isPublic || function.isInternal
            "all" -> true
            else -> function.isPublic
        }
        if (!shouldCheck) return

        // If there is a modifier param, we bail
        if (function.modifierParameter(config) != null) return

        // In case we didn't find any `modifier` parameters, we check if it emits content and report the error if so.
        if (function.emitsContent(config)) {
            emitter.report(function, MissingModifierContentComposable)
        }
    }

    companion object {
        val MissingModifierContentComposable = """
            This @Composable function emits content but doesn't have a modifier parameter.
            See https://mrmans0n.github.io/compose-rules/rules/#when-should-i-expose-modifier-parameters for more information.
        """.trimIndent()
    }
}
