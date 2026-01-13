// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core.util

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

val KtFunction.returnsValue: Boolean
    get() = typeReference != null && typeReference!!.text != "Unit"

val KtFunction.hasReceiverType: Boolean
    get() = receiverTypeReference != null

val KtModifierListOwner.isPrivate: Boolean
    get() = visibilityModifierType() == KtTokens.PRIVATE_KEYWORD

val KtModifierListOwner.isProtected: Boolean
    get() = visibilityModifierType() == KtTokens.PROTECTED_KEYWORD

val KtModifierListOwner.isInternal: Boolean
    get() = visibilityModifierType() == KtTokens.INTERNAL_KEYWORD

val KtFunction.isOverride: Boolean
    get() = hasModifier(KtTokens.OVERRIDE_KEYWORD)

val KtFunction.isActual: Boolean
    get() = hasModifier(KtTokens.ACTUAL_KEYWORD)

val KtFunction.isExpect: Boolean
    get() = hasModifier(KtTokens.EXPECT_KEYWORD)

val KtFunction.isAbstract: Boolean
    get() = hasModifier(KtTokens.ABSTRACT_KEYWORD)

val KtFunction.isOpen: Boolean
    get() = hasModifier(KtTokens.OPEN_KEYWORD)

val KtFunction.isOperator: Boolean
    get() = hasModifier(KtTokens.OPERATOR_KEYWORD)

val KtFunction.definedInInterface: Boolean
    get() = ((parent as? KtClassBody)?.parent as? KtClass)?.isInterface() ?: false

val KtNamedFunction.isNested: Boolean
    get() = parents.takeWhile { it !is KtFile }.any { it is KtNamedFunction }

/**
 * Returns true if the function has any context receivers or context parameters.
 *
 * Context receivers use the old syntax: `context(ColumnScope)`
 * Context parameters use the new syntax: `context(columnScope: ColumnScope)`
 *
 * This property is backward compatible with older Kotlin compiler versions that don't
 * support the contextReceiverList API. In those versions, it falls back to checking
 * the deprecated contextReceivers property (which only supports the old syntax).
 */
val KtNamedFunction.hasAnyContextArguments: Boolean
    get() = try {
        val contextReceiverList = contextReceiverList ?: return false
        contextReceiverList.contextReceivers().isNotEmpty() ||
            contextReceiverList.contextParameters().isNotEmpty()
    } catch (_: NoSuchMethodError) {
        // contextReceiverList is not available in older Kotlin compiler versions
        // Fall back to the deprecated contextReceivers property
        try {
            @Suppress("DEPRECATION")
            contextReceivers.isNotEmpty()
        } catch (e: NoSuchMethodError) {
            // If even contextReceivers is not available, assume no context arguments
            false
        }
    }
