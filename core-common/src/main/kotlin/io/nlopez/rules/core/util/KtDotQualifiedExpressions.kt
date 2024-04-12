// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

val KtDotQualifiedExpression.rootExpression: KtExpression
    get() {
        var current: KtExpression = receiverExpression
        while (current is KtDotQualifiedExpression) {
            current = current.receiverExpression
        }
        return current
    }

/**
 * [KtDotQualifiedExpression] can be nested, and if we only care about the one that contains all the expression,
 * this method will filter out all the others from the [Sequence].
 *
 * For example: "androidx.compose.material" would have "androidx.compose.material", "androidx.compose" and "androidx".
 * If these were in the same sequence, we'd only use "androidx.compose.material" and get rid of the others.
 */
fun Sequence<KtDotQualifiedExpression>.dedupUsingOutermost(): Sequence<KtDotQualifiedExpression> =
    map { it.outermost }.distinct()

val KtDotQualifiedExpression.outermost: KtDotQualifiedExpression
    get() = parentsWithSelf.takeWhile { it is KtDotQualifiedExpression }.last() as KtDotQualifiedExpression
