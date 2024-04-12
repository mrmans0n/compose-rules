// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtConstantExpression

val KtConstantExpression.isInt: Boolean
    get() = isIntegerConstant && !isLong

val KtConstantExpression.isLong: Boolean
    get() = isIntegerConstant && text.endsWith("L")

val KtConstantExpression.isDouble: Boolean
    get() = isFloatConstant && !isFloat

val KtConstantExpression.isFloat: Boolean
    get() = isFloatConstant && text.endsWith("f")

private val KtConstantExpression.isIntegerConstant: Boolean
    get() = node.elementType == KtNodeTypes.INTEGER_CONSTANT

private val KtConstantExpression.isFloatConstant: Boolean
    get() = node.elementType == KtNodeTypes.FLOAT_CONSTANT
