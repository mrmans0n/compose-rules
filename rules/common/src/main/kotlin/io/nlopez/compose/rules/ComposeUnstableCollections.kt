// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.isTypeUnstableCollection
import org.jetbrains.kotlin.psi.KtFunction
import java.util.*

class ComposeUnstableCollections : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter) {
        for (param in function.valueParameters.filter { it.isTypeUnstableCollection }) {
            val variableName = param.nameAsSafeName.asString()
            val type = param.typeReference?.text ?: "List/Set/Map"
            val message = createErrorMessage(
                type = type,
                rawType = type.replace(DiamondRegex, ""),
                variable = variableName,
            )
            emitter.report(param.typeReference ?: param, message)
        }
    }

    companion object {
        private val DiamondRegex by lazy(LazyThreadSafetyMode.NONE) { Regex("<.*>\\??") }
        private val String.capitalized: String
            get() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        fun createErrorMessage(type: String, rawType: String, variable: String) = """
            The Compose Compiler cannot infer the stability of a parameter if a $type is used in it, even if the item type is stable.
            You should use Kotlinx Immutable Collections instead: `$variable: Immutable$type` or create an `@Immutable` wrapper for this class: `@Immutable data class ${variable.capitalized}$rawType(val items: $type)`

            See https://mrmans0n.github.io/compose-rules/rules/#avoid-using-unstable-collections for more information.
        """.trimIndent()
    }
}
