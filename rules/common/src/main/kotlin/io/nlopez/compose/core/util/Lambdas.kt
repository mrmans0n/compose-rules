// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core.util

import io.nlopez.compose.core.ComposeKtConfig
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

fun KtTypeElement.isLambda(treatAsLambdaTypes: Set<String> = emptySet()): Boolean = when (this) {
    is KtFunctionType -> true
    is KtNullableType -> innerType?.isLambda(treatAsLambdaTypes) == true
    is KtUserType -> referencedName in treatAsLambdaTypes
    else -> false
}

fun KtTypeReference.isLambda(treatAsLambdaTypes: Set<String> = emptySet()): Boolean =
    typeElement?.isLambda(treatAsLambdaTypes) == true

fun KtTypeReference.isComposableLambda(
    treatAsLambdaTypes: Set<String> = emptySet(),
    treatAsComposableLambdaTypes: Set<String> = emptySet(),
): Boolean = when (val element = typeElement) {
    null -> false

    is KtFunctionType -> isComposable

    is KtNullableType -> (isComposable && element.isLambda(treatAsLambdaTypes)) ||
        // Only possibility for this to not have a @Composable annotation is for it to be a KtUserType
        (element.innerType as? KtUserType)?.referencedName in treatAsComposableLambdaTypes

    is KtUserType -> (isComposable && element.referencedName in treatAsLambdaTypes) ||
        element.referencedName in treatAsComposableLambdaTypes

    else -> false
}

fun KtTypeReference.isComposableUiEmitterLambda(
    treatAsLambdaTypes: Set<String> = emptySet(),
    treatAsComposableLambdaTypes: Set<String> = emptySet(),
): Boolean = when (val element = typeElement) {
    null -> false

    is KtFunctionType -> isComposable && element.returnsUnit

    is KtNullableType -> (isComposable && element.isLambda(treatAsLambdaTypes)) ||
        // Only possibility for this to not have a @Composable annotation is for it to be a KtUserType
        (element.innerType as? KtUserType)?.referencedName in treatAsComposableLambdaTypes

    is KtUserType -> (isComposable && element.referencedName in treatAsLambdaTypes) ||
        element.referencedName in treatAsComposableLambdaTypes

    else -> false
}

val KtTypeElement.returnsUnit: Boolean
    get() = when (this) {
        is KtFunctionType -> returnTypeReference?.text == "Unit"
        is KtNullableType -> innerType?.returnsUnit == true
        else -> false
    }

val KtTypeReference.returnsUnit: Boolean
    get() = when (val element = typeElement) {
        null -> false
        is KtFunctionType -> element.returnsUnit
        is KtNullableType -> element.innerType?.returnsUnit == true
        else -> false
    }

fun KtFile.lambdaTypes(config: ComposeKtConfig): Set<String> = buildSet {
    // Add the provided types
    addAll(config.getSet("treatAsLambda", emptySet()))

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

fun KtFile.composableLambdaTypes(config: ComposeKtConfig): Set<String> = buildSet {
    // Add the provided types
    addAll(config.getSet("treatAsComposableLambda", emptySet()))

    // Add fun interfaces that have their sam method as composable
    addAll(
        findChildrenByClass<KtClass>()
            .filter { it.isInterface() && it.hasModifier(KtTokens.FUN_KEYWORD) }
            .filter { funInterface ->
                // Find if the method that has no implementation (aka the SAM) is @Composable
                funInterface.body
                    ?.functions
                    ?.filterNot { it.hasBody() }
                    ?.map { it.isComposable }
                    ?.firstOrNull() ?: false
            }
            .mapNotNull { it.name },
    )

    // Add typealias with functional types
    // NOTE: it has to be last, so that isLambda picks up fun interfaces / config stuff in lambdaTypes
    addAll(
        findChildrenByClass<KtTypeAlias>()
            .filter {
                val typeReference = it.getTypeReference() ?: return@filter false
                when (val typeElement = typeReference.typeElement) {
                    null -> false

                    // typealias A = @Composable () -> Unit
                    is KtFunctionType -> typeReference.isComposable

                    // typealias B = @Composable (() -> Unit?)
                    // typealias B = A?
                    is KtNullableType -> {
                        (typeReference.isComposable && typeElement.innerType is KtFunctionType) ||
                            typeElement.innerType?.name in this
                    }

                    // typealias C = A
                    is KtUserType -> typeElement.referencedName in this

                    else -> false
                }
            }
            .mapNotNull { it.name },
    )
}
