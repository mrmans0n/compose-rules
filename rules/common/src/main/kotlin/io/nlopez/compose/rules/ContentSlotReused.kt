// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.composableLambdaTypes
import io.nlopez.compose.core.util.contentSlots
import io.nlopez.compose.core.util.findAllChildrenByClass
import io.nlopez.compose.core.util.findShadowingRedeclarations
import io.nlopez.compose.core.util.isTypeNullable
import io.nlopez.compose.core.util.lambdaTypes
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression

class ContentSlotReused : ComposeKtVisitor {
    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        val lambdaTypes = function.containingKtFile.lambdaTypes(config)
        val composableLambdaTypes = function.containingKtFile.composableLambdaTypes(config)

        val slotsWithMultipleUsages = function.contentSlots(lambdaTypes, composableLambdaTypes)
            .filter { slot -> function.findNotShadowedUsagesOf(slot).count() >= 2 }

        for (slot in slotsWithMultipleUsages) {
            emitter.report(slot, ContentSlotsShouldNotBeReused)
        }
    }

    private fun KtFunction.findNotShadowedUsagesOf(slot: KtParameter): Sequence<KtCallExpression> {
        val slotName = slot.name?.takeIf { it.isNotEmpty() } ?: return emptySequence()
        val slots = when {
            // content?.invoke()
            slot.isTypeNullable -> findAllChildrenByClass<KtSafeQualifiedExpression>()
                .filter { it.receiverExpression.text == slotName }
                .mapNotNull { it.selectorExpression as? KtCallExpression }
                .filter { it.calleeExpression?.text == "invoke" }

            // content()
            else -> findAllChildrenByClass<KtCallExpression>().filter { it.calleeExpression?.text == slotName }
        }
        // Return and remove shadowed usages
        return slots.filter { it.findShadowingRedeclarations(parameterName = slotName, stopAt = this).count() == 0 }
    }

    companion object {
        val ContentSlotsShouldNotBeReused = """
            Content slots should not be reused in different code branches/scopes of a composable function, to preserve the slot internal state.
            You can wrap the slot in a remember { movableContentOf { ... }} block to make sure their internal state is preserved correctly.
            See https://mrmans0n.github.io/compose-rules/rules/#content-slots-should-not-be-reused-in-branching-code for more information.
        """.trimIndent()
    }
}
