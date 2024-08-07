// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core.util

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.util.Deque
import java.util.LinkedList

val PsiElementAlwaysTruePredicate: (PsiElement) -> Boolean = { true }

inline fun <reified T : PsiElement> PsiElement.findChildrenByClass(
    noinline shouldVisitChildren: (PsiElement) -> Boolean = PsiElementAlwaysTruePredicate,
): Sequence<T> = sequence {
    val queue: Deque<PsiElement> = LinkedList()
    queue.add(this@findChildrenByClass)
    while (queue.isNotEmpty()) {
        val current = queue.pop()
        if (current is T) {
            yield(current)
        }
        if (shouldVisitChildren(current)) {
            queue.addAll(current.children)
        }
    }
}

inline fun <reified T : PsiElement> PsiElement.findDirectFirstChildByClass(): T? {
    var current = firstChild
    while (current != null) {
        if (current is T) {
            return current
        }
        current = current.nextSibling
    }
    return null
}

inline fun <reified T : PsiElement> PsiElement.findDirectChildrenByClass(): Sequence<T> = sequence {
    var current = firstChild
    while (current != null) {
        if (current is T) {
            yield(current)
        }
        current = current.nextSibling
    }
}

fun PsiElement.walkBackwards(stopAtParent: PsiElement? = null): Sequence<PsiElement> = parentsWithSelf
    .flatMap { it.siblings(forward = false, withItself = true) }
    .takeWhile { it != stopAtParent }

val PsiNameIdentifierOwner.startOffsetFromName: Int
    get() = nameIdentifier?.startOffset ?: startOffset

val PsiElement.range: IntRange
    get() = IntRange(startOffset, endOffset)
