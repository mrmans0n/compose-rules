// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.composableLambdaTypes
import io.nlopez.compose.core.util.contentSlots
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.findShadowingRedeclarations
import io.nlopez.compose.core.util.lambdaTypes
import io.nlopez.compose.core.util.uniquePairs
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

class ContentSlotReused : ComposeKtVisitor {
    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        val lambdaTypes = function.containingKtFile.lambdaTypes(config)
        val composableLambdaTypes = function.containingKtFile.composableLambdaTypes(config)

        val slots = function.contentSlots(lambdaTypes, composableLambdaTypes)
            .filter { it.name?.isNotEmpty() == true }

        val slotsWithMultipleUsages = slots.filter { it.name != null }
            .map { slot -> slot to function.findUsages(slot.name!!) }
            .filter { (_, usages) -> usages.count() >= 2 }

        // Now that we found some candidates, we need to make sure the slots being reused are in different branches
        // in the ast tree. We'll need to search for parent ifs, whens, etc.
        val slotsInDifferentBranches = slotsWithMultipleUsages
            .filter { (_, usages) -> isSlotUsedInSeparateBranches(usages, function) }
            .map { (slot, _) -> slot }

        for (slot in slotsInDifferentBranches) {
            emitter.report(slot, ContentSlotReusedInDifferentBranches)
        }
    }

    private fun KtFunction.findUsages(name: String): Sequence<KtCallExpression> =
        findChildrenByClass<KtCallExpression>().filter { it.calleeExpression?.text == name }
            // Remove shadowed usages
            .filter { it.findShadowingRedeclarations(name, this).count() == 0 }

    private fun isSlotUsedInSeparateBranches(slots: Sequence<KtCallExpression>, stopAt: KtFunction): Boolean =
        slots.uniquePairs()
            .any { (slot1, slot2) ->
                findCommonAncestor(slot1, slot2, stopAt).isBranchingElement()
            }

    private fun findCommonAncestor(slot1: KtCallExpression, slot2: KtCallExpression, stopAt: KtFunction): KtElement {
        val height1 = slot1.parents.takeWhile { it != stopAt }.count()
        val height2 = slot2.parents.takeWhile { it != stopAt }.count()

        var current1: KtElement = slot1
        var current2: KtElement = slot2

        // If the heights are different, we'll need to go up in the one that's deeper until they
        when {
            height1 > height2 -> {
                repeat(height1 - height2) { current1 = current1.parent as KtElement }
            }

            height1 < height2 -> {
                repeat(height2 - height1) { current2 = current2.parent as KtElement }
            }
        }

        // Traverse up until they are at the same level
        while (current1 != current2) {
            current1 = current1.parent as KtElement
            current2 = current2.parent as KtElement
        }

        return current1
    }

    private fun KtElement.isBranchingElement(): Boolean = when (val current = this) {
        // Always true, if not, the ancestor would have been found in then or in else expressions
        is KtIfExpression -> true
        // Always true, if not, the ancestor would have been found in a when entry
        is KtWhenExpression -> true
        // Only branching if it's an elvis operator
        is KtBinaryExpression -> current.operationToken == KtTokens.ELVIS
        else -> false
    }

    companion object {
        val ContentSlotReusedInDifferentBranches = """
            Content slots should not be reused in different code branches of a composable function (e.g. if/when/elvis).

            You can wrap the usages in a remember { movableContentOf { ... }} block to make sure their internal state is preserved correctly.

            See https://mrmans0n.github.io/compose-rules/rules/#content-slots-should-not-be-reused-in-branching-code for more information.
        """.trimIndent()
    }
}
