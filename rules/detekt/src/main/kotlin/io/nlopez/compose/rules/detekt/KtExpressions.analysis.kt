// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal fun KtExpression.isSameResolvedValueAs(other: KtExpression): Boolean = runCatching {
    analyze(this) {
        val resolver = object {
            fun KtExpression.resolvedValueKey(): ResolvedValueKey? {
                val symbol = when (this) {
                    is KtSimpleNameExpression -> mainReference.resolveToSymbol()

                    is KtQualifiedExpression -> (selectorExpression as? KtSimpleNameExpression)
                        ?.mainReference
                        ?.resolveToSymbol()

                    else -> (resolveToCall() as? KaVariableAccessCall)?.signature?.symbol
                } ?: return null
                val symbolKey = symbol.sourcePsiSafe<KtElement>() ?: symbol

                val receiverKey = when (this) {
                    is KtQualifiedExpression -> receiverExpression.resolvedReceiverKey() ?: return null
                    else -> null
                }
                return ResolvedValueKey(symbolKey, receiverKey)
            }

            private fun KtExpression.resolvedReceiverKey(): Any? = when (this) {
                is KtSimpleNameExpression -> {
                    val symbol = mainReference.resolveToSymbol() ?: return null
                    symbol.sourcePsiSafe<KtElement>() ?: symbol
                }

                is KtQualifiedExpression -> resolvedValueKey()

                else -> null
            }
        }

        with(resolver) {
            val left = this@isSameResolvedValueAs.resolvedValueKey()
            val right = other.resolvedValueKey()
            left != null && right != null && left == right
        }
    }
}.getOrDefault(false)

private data class ResolvedValueKey(val symbolKey: Any, val receiverKey: Any?)
