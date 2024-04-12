// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.report
import io.nlopez.rules.core.util.findChildrenByClass
import io.nlopez.rules.core.util.isDouble
import io.nlopez.rules.core.util.isFloat
import io.nlopez.rules.core.util.isInt
import io.nlopez.rules.core.util.isLong
import io.nlopez.rules.core.util.mapFirst
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression

class MutableStateAutoboxing : ComposeKtVisitor {

    override fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {
        // Things we can realistically support without type resolution without going bananas
        // - numeric constants
        // - stuff that comes from function params
        val allMutableStateOfWithConstantSingleArgument = file.findMutableStateOf()
            .filter { it.singleArgumentExpression is KtConstantExpression }
            .map { it to it.singleArgumentExpression as KtConstantExpression }

        // mutableIntStateOf
        val ints = allMutableStateOfWithConstantSingleArgument
            .filter { (_, constantExpression) -> constantExpression.isInt }
            .mapFirst()

        for (item in ints) {
            emitter.report(item, MutableStateAutoboxingInt)
        }

        // mutableLongStateOf
        val longs = allMutableStateOfWithConstantSingleArgument
            .filter { (_, constantExpression) -> constantExpression.isLong }
            .mapFirst()

        for (item in longs) {
            emitter.report(item, MutableStateAutoboxingLong)
        }

        // mutableDoubleStateOf
        val doubles = allMutableStateOfWithConstantSingleArgument
            .filter { (_, constantExpression) -> constantExpression.isDouble }
            .mapFirst()

        for (item in doubles) {
            emitter.report(item, MutableStateAutoboxingDouble)
        }

        // mutableFloatStateOf
        val floats = allMutableStateOfWithConstantSingleArgument
            .filter { (_, constantExpression) -> constantExpression.isFloat }
            .mapFirst()

        for (item in floats) {
            emitter.report(item, MutableStateAutoboxingFloat)
        }
    }

    override fun visitFunction(function: KtFunction, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {
        // Find the relevant names associated with the types we want (Int, Long, Double, Float)
        val namesAndTypes = function.valueParameters
            .filter { it.typeReference?.text in SupportedTypes }
            .associateBy(
                keySelector = { it.nameAsSafeName.asString() },
                valueTransform = { it.typeReference!!.text },
            )

        // Find the mutableStateOf(x) where x is any of those parameter names we found before
        val candidates = function.findMutableStateOf()
            .filter { it.singleArgumentExpression is KtReferenceExpression }
            .filter { it.singleArgumentExpression?.text in namesAndTypes.keys }

        // mutableIntStateOf
        val ints = candidates
            .filter { namesAndTypes[it.singleArgumentExpression?.text] == "Int" }

        for (item in ints) {
            emitter.report(item, MutableStateAutoboxingInt)
        }

        // mutableLongStateOf
        val longs = candidates
            .filter { namesAndTypes[it.singleArgumentExpression?.text] == "Long" }

        for (item in longs) {
            emitter.report(item, MutableStateAutoboxingLong)
        }

        // mutableDoubleStateOf
        val doubles = candidates
            .filter { namesAndTypes[it.singleArgumentExpression?.text] == "Double" }

        for (item in doubles) {
            emitter.report(item, MutableStateAutoboxingDouble)
        }

        // mutableFloatStateOf
        val floats = candidates
            .filter { namesAndTypes[it.singleArgumentExpression?.text] == "Float" }

        for (item in floats) {
            emitter.report(item, MutableStateAutoboxingFloat)
        }
    }

    private val KtCallExpression.singleArgumentExpression: KtExpression?
        get() = valueArguments.singleOrNull()?.getArgumentExpression()

    private fun PsiElement.findMutableStateOf() = findChildrenByClass<KtCallExpression>()
        .filter { it.calleeExpression?.text == "mutableStateOf" }
        .filter { it.valueArguments.size == 1 }

    companion object {
        private val SupportedTypes = setOf("Int", "Long", "Float", "Double")
        val MutableStateAutoboxingInt = """
            Using mutableIntStateOf is recommended over mutableStateOf<Int>, as it uses the Int primitive directly which is more performant.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingLong = """
            Using mutableLongStateOf is recommended over mutableStateOf<Long>, as it uses the Long primitive directly which is more performant.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingDouble = """
            Using mutableDoubleStateOf is recommended over mutableStateOf<Double>, as it uses the Double primitive directly which is more performant.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingFloat = """
            Using mutableFloatStateOf is recommended over mutableStateOf<Float>, as it uses the Float primitive directly which is more performant.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()
    }
}
