// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.dedupUsingOutermost
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.plus
import io.nlopez.compose.core.util.range
import io.nlopez.compose.core.util.runIfNotNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class Material2 : ComposeKtVisitor {
    override val isOptIn: Boolean = true

    override fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {
        // Allowed elements/apis from material2, in the format of whatever comes after androidx.compose.material
        // For instance, if we want to allow icons, we'll put just `Icons`, or if we only allowed the filled icons,
        // we'll put `icons.filled` (regardless of the specifics that could come after)
        val allowedFqNames = config.getSet("allowedFromM2", emptySet()).map { M2FqName + it } + NotInM2

        // Find in import list
        val imports = file.importList?.imports.orEmpty()
            .filterNotNull()
            .filter { it.importedFqName?.startsWith(M2FqName) == true }
            .filterNot { directive ->
                allowedFqNames.any { directive.importedFqName?.startsWith(it) == true }
            }

        for (directive in imports) {
            emitter.report(directive, DisallowedUsageOfMaterial2)
        }

        // Find usages that don't need imports (e.g. androidx.compose.material.Icons.Arrow being used directly)
        val dotQualified = file.findChildrenByClass<KtDotQualifiedExpression>()
            // Ignore the ones in imports
            .runIfNotNull(file.importList) { imps -> filter { it.startOffset !in imps.range } }
            // Ignore the ones in package definitions
            .runIfNotNull(file.packageDirective) { pkg -> filter { it.startOffset !in pkg.range } }
            // De-dup nested KtDotQualifiedExpression and only process the outermost
            .dedupUsingOutermost()
            .filter { it.hasReferenceToM2(allowedFqNames) }

        for (reference in dotQualified) {
            emitter.report(reference, DisallowedUsageOfMaterial2)
        }
    }

    private fun KtDotQualifiedExpression.hasReferenceToM2(allowlist: List<FqName>): Boolean {
        // 2 possible routes here:
        // - the reference expression is a KtCallExpression (e.g. `androidx.compose.material.Text(...)`)
        // - the reference expression is a KtReferenceExpression (e.g. `androidx.compose.material.Icons.Arrow`)
        // It needs to be in that order, as KtCallExpression is a KtReferenceExpression.
        return when (val expression = selectorExpression) {
            is KtCallExpression -> {
                runCatching {
                    val fqn = FqName(receiverExpression.text + "." + expression.calleeExpression?.text)
                    fqn.startsWith(M2FqName) && allowlist.none { fqn.startsWith(it) }
                }.getOrDefault(false)
            }

            is KtReferenceExpression -> {
                runCatching {
                    val fqn = FqName(text)
                    fqn.startsWith(M2FqName) && allowlist.none { fqn.startsWith(it) }
                }.getOrDefault(false)
            }

            else -> false
        }
    }

    companion object {
        private val M2FqName = FqName.fromSegments(listOf("androidx", "compose", "material"))
        private val NotInM2 = setOf(
            M2FqName + "icons",
        )
        val DisallowedUsageOfMaterial2 = """
            Compose Material 2 is disallowed by your configuration.

            See https://mrmans0n.github.io/compose-rules/rules/#dont-use-material-2 for more information.
        """.trimIndent()
    }
}
