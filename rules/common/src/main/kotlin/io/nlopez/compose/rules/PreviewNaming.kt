// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.ifFix
import io.nlopez.compose.core.util.isPreview
import org.jetbrains.kotlin.psi.KtFunction

class PreviewNaming : ComposeKtVisitor {
    override val isOptIn: Boolean = true

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        if (!function.isPreview) return

        val strategy = when (config.getString("previewNamingStrategy", "suffix")) {
            "suffix" -> PreviewNamingType.Suffix
            "prefix" -> PreviewNamingType.Prefix
            "anywhere" -> PreviewNamingType.Anywhere
            else -> PreviewNamingType.Suffix
        }

        val name = function.nameAsSafeName.asString()
        // By default, we expect Preview to be the suffix. It can be configured to be the prefix though.

        when (strategy) {
            PreviewNamingType.Suffix -> {
                if (!name.endsWith("Preview")) {
                    emitter.report(function, PreviewDoesNotEndWithPreview, true).ifFix {
                        function.setName("${name}Preview")
                    }
                }
            }

            PreviewNamingType.Prefix -> {
                if (!name.startsWith("Preview")) {
                    emitter.report(function, PreviewDoesNotStartWithPreview, true).ifFix {
                        function.setName("Preview$name")
                    }
                }
            }

            PreviewNamingType.Anywhere -> {
                if (!name.contains("Preview")) {
                    emitter.report(function, PreviewDoesNotContainPreview, true).ifFix {
                        // If anywhere is fine, we'll just add it as suffix \_(ツ)_/
                        function.setName("${name}Preview")
                    }
                }
            }
        }
    }

    private enum class PreviewNamingType {
        Suffix,
        Prefix,
        Anywhere,
    }

    companion object {
        val PreviewDoesNotStartWithPreview = """
            Preview functions should have `Preview` as prefix, per your project's configuration.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-previews-properly for more information.
        """.trimIndent()

        val PreviewDoesNotEndWithPreview = """
            Preview functions should have `Preview` as suffix, per your project's configuration.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-previews-properly for more information.
        """.trimIndent()

        val PreviewDoesNotContainPreview = """
            Preview functions should contain `Preview` in their names, per your project's configuration.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-previews-properly for more information.
        """.trimIndent()
    }
}
