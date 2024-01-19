// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import io.nlopez.rules.core.ComposeKtConfig
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

fun KtTypeElement.isLambda(treatAsLambdaTypes: Set<String>): Boolean = when (this) {
    is KtFunctionType -> true
    is KtNullableType -> innerType?.isLambda(treatAsLambdaTypes) == true
    is KtUserType -> getReferencedName() in treatAsLambdaTypes
    else -> false
}

fun KtTypeReference.isLambda(treatAsLambdaTypes: Set<String>): Boolean =
    typeElement?.isLambda(treatAsLambdaTypes) == true

context(ComposeKtConfig)
val KtFile.lambdaTypes: Set<String>
    get() = buildSet {
        // Add the provided types
        addAll(getSet("treatAsLambda", emptySet()))

        // Add fun interfaces
        addAll(
            findChildrenByClass<KtClass>()
                .filter { it.isInterface() && it.hasModifier(KtTokens.FUN_KEYWORD) }
                .mapNotNull { it.name },
        )

        // Add typealias with functional types
        // NOTE: it has to be last, so that isLambda picks up fun interfaces / config stuff in lambdaTypes
        addAll(
            findChildrenByClass<KtTypeAlias>()
                .filter { it.getTypeReference()?.isLambda(this) == true }
                .mapNotNull { it.name },
        )
    }
