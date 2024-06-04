// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.isDouble
import io.nlopez.compose.core.util.isFloat
import io.nlopez.compose.core.util.isInt
import io.nlopez.compose.core.util.isLong
import io.nlopez.compose.core.util.mapFirst
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression

class MutableStateAutoboxing : ComposeKtVisitor {

    override fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {
        // Things we can realistically support without type resolution without going bananas
        // - numeric constants
        // - explicit types
        // - stuff that comes from function params
        val msofs = file.findMutableStateOf()

        // Let's try inferring by the constant expressions (e.g. mutableStateOf(0) or mutableStateOf(3f)
        val allMutableStateOfWithConstantSingleArgument = msofs
            .filter { it.singleArgumentExpression is KtConstantExpression }
            .map { it to it.singleArgumentExpression as KtConstantExpression }

        // mutableIntStateOf
        val ints = allMutableStateOfWithConstantSingleArgument
            .mapCallExpressionIfConstantExpression { it.isInt }

        for (item in ints) {
            emitter.report(item, MutableStateAutoboxingInt)
        }

        // mutableLongStateOf
        val longs = allMutableStateOfWithConstantSingleArgument
            .mapCallExpressionIfConstantExpression { it.isLong }

        for (item in longs) {
            emitter.report(item, MutableStateAutoboxingLong)
        }

        // mutableDoubleStateOf
        val doubles = allMutableStateOfWithConstantSingleArgument
            .mapCallExpressionIfConstantExpression { it.isDouble }

        for (item in doubles) {
            emitter.report(item, MutableStateAutoboxingDouble)
        }

        // mutableFloatStateOf
        val floats = allMutableStateOfWithConstantSingleArgument
            .mapCallExpressionIfConstantExpression { it.isFloat }

        for (item in floats) {
            emitter.report(item, MutableStateAutoboxingFloat)
        }
    }

    private inline fun Sequence<Pair<KtCallExpression, KtConstantExpression>>.mapCallExpressionIfConstantExpression(
        crossinline predicate: (KtConstantExpression) -> Boolean,
    ): Sequence<KtCallExpression> = filter { (_, constantExpression) -> predicate(constantExpression) }.mapFirst()

    override fun visitFunction(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        // Find the relevant parameter names associated with the types we want (Int, Long, Double, Float)
        val parameterNamesAndTypes = function.valueParameters
            .filter { it.typeReference?.text in SupportedTypes }
            .associateBy(
                keySelector = { it.nameAsSafeName.asString() },
                valueTransform = { it.typeReference!!.text },
            )

        val mutableStateOfReferences = function.findMutableStateOf()
            .filter { it.singleArgumentExpression is KtReferenceExpression }

        // Find the mutableStateOf(x) where x is any of those parameter names we found before
        val primitiveCandidates = mutableStateOfReferences
            .filter { it.singleArgumentExpression?.text in parameterNamesAndTypes.keys }

        fun emitIfParameterTypeIs(message: String, predicate: (String) -> Boolean) {
            val matches = primitiveCandidates.filter {
                parameterNamesAndTypes[it.singleArgumentExpression?.text]?.let { type -> predicate(type) } ?: false
            }
            for (match in matches) {
                emitter.report(match, message)
            }
        }

        // mutableIntStateOf
        emitIfParameterTypeIs(MutableStateAutoboxingInt) { it == "Int" }

        // mutableLongStateOf
        emitIfParameterTypeIs(MutableStateAutoboxingLong) { it == "Long" }

        // mutableDoubleStateOf
        emitIfParameterTypeIs(MutableStateAutoboxingDouble) { it == "Double" }

        // mutableFloatStateOf
        emitIfParameterTypeIs(MutableStateAutoboxingFloat) { it == "Float" }

        // mutableIntListOf
        emitIfParameterTypeIs(MutableStateAutoboxingIntList) {
            it == "List<Int>" || it == "PersistentList<Int>" || it == "ImmutableList<Int>"
        }

        // mutableLongListOf
        emitIfParameterTypeIs(MutableStateAutoboxingLongList) {
            it == "List<Long>" || it == "PersistentList<Long>" || it == "ImmutableList<Long>"
        }

        // mutableFloatListOf
        emitIfParameterTypeIs(MutableStateAutoboxingFloatList) {
            it == "List<Float>" || it == "PersistentList<Float>" || it == "ImmutableList<Float>"
        }

        // mutableIntSetOf
        emitIfParameterTypeIs(MutableStateAutoboxingIntSet) {
            it == "Set<Int>" || it == "PersistentSet<Int>" || it == "ImmutableSet<Int>"
        }

        // mutableLongSetOf
        emitIfParameterTypeIs(MutableStateAutoboxingLongSet) {
            it == "Set<Long>" || it == "PersistentSet<Long>" || it == "ImmutableSet<Long>"
        }

        // mutableFloatSetOf
        emitIfParameterTypeIs(MutableStateAutoboxingFloatSet) {
            it == "Set<Float>" || it == "PersistentSet<Float>" || it == "ImmutableSet<Float>"
        }

        // mutableIntIntMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingIntIntMap) {
            it == "Map<Int, Int>" || it == "PersistentMap<Int, Int>" || it == "ImmutableMap<Int, Int>"
        }

        // mutableIntLongMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingIntLongMap) {
            it == "Map<Int, Long>" || it == "PersistentMap<Int, Long>" || it == "ImmutableMap<Int, Long>"
        }

        // mutableIntFloatMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingIntFloatMap) {
            it == "Map<Int, Float>" || it == "PersistentMap<Int, Float>" || it == "ImmutableMap<Int, Float>"
        }

        // mutableLongIntMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingLongIntMap) {
            it == "Map<Long, Int>" || it == "PersistentMap<Long, Int>" || it == "ImmutableMap<Long, Int>"
        }

        // mutableLongLongMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingLongLongMap) {
            it == "Map<Long, Long>" || it == "PersistentMap<Long, Long>" || it == "ImmutableMap<Long, Long>"
        }

        // mutableLongFloatMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingLongFloatMap) {
            it == "Map<Long, Float>" || it == "PersistentMap<Long, Float>" || it == "ImmutableMap<Long, Float>"
        }

        // mutableFloatIntMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingFloatIntMap) {
            it == "Map<Float, Int>" || it == "PersistentMap<Float, Int>" || it == "ImmutableMap<Float, Int>"
        }

        // mutableFloatLongMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingFloatLongMap) {
            it == "Map<Float, Long>" || it == "PersistentMap<Float, Long>" || it == "ImmutableMap<Float, Long>"
        }

        // mutableFloatFloatMapOf
        emitIfParameterTypeIs(MutableStateAutoboxingFloatFloatMap) {
            it == "Map<Float, Float>" || it == "PersistentMap<Float, Float>" || it == "ImmutableMap<Float, Float>"
        }

        // TODO Add support to mutableObjectListOf, mutableObjectSetOf, mutableObjectMapOf, mutableObjectFloatMapOf,
        //  mutableObjectLongMapOf, mutableObjectIntMapOf
    }

    private val KtCallExpression.singleArgumentExpression: KtExpression?
        get() = valueArguments.singleOrNull()?.getArgumentExpression()

    private fun PsiElement.findMutableStateOf() = findChildrenByClass<KtCallExpression>()
        .filter { it.calleeExpression?.text == "mutableStateOf" }
        .filter { it.valueArguments.size == 1 }

    companion object {
        private val SupportedTypes = setOf(
            "Int",
            "Long",
            "Float",
            "Double",
            // Lists
            "List<Int>",
            "PersistentList<Int>",
            "ImmutableList<Int>",
            "List<Long>",
            "PersistentList<Long>",
            "ImmutableList<Long>",
            "List<Float>",
            "PersistentList<Float>",
            "ImmutableList<Float>",
            // Sets
            "Set<Int>",
            "PersistentSet<Int>",
            "ImmutableSet<Int>",
            "Set<Long>",
            "PersistentSet<Long>",
            "ImmutableSet<Long>",
            "Set<Float>",
            "PersistentSet<Float>",
            "ImmutableSet<Float>",
            // Maps
            "Map<Int, Int>",
            "PersistentMap<Int, Int>",
            "ImmutableMap<Int, Int>",
            "Map<Int, Long>",
            "PersistentMap<Int, Long>",
            "ImmutableMap<Int, Long>",
            "Map<Int, Float>",
            "PersistentMap<Int, Float>",
            "ImmutableMap<Int, Float>",
            "Map<Long, Int>",
            "PersistentMap<Long, Int>",
            "ImmutableMap<Long, Int>",
            "Map<Long, Long>",
            "PersistentMap<Long, Long>",
            "ImmutableMap<Long, Long>",
            "Map<Long, Float>",
            "PersistentMap<Long, Float>",
            "ImmutableMap<Long, Float>",
            "Map<Float, Int>",
            "PersistentMap<Float, Int>",
            "ImmutableMap<Float, Int>",
            "Map<Float, Long>",
            "PersistentMap<Float, Long>",
            "ImmutableMap<Float, Long>",
            "Map<Float, Float>",
            "PersistentMap<Float, Float>",
            "ImmutableMap<Float, Float>",
        )

        // Primitives
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

        // Primitive Lists
        val MutableStateAutoboxingIntList = """
            Using mutableIntListOf is recommended over mutableStateOf Immutable/Persistent/List<Int> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingLongList = """
            Using mutableLongListOf is recommended over mutableStateOf Immutable/Persistent/List<Long> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingFloatList = """
            Using mutableFloatListOf is recommended over mutableStateOf Immutable/Persistent/List<Float> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        // Primitive Sets
        val MutableStateAutoboxingIntSet = """
            Using mutableIntSetOf is recommended over mutableStateOf Immutable/Persistent/Set<Int> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingLongSet = """
            Using mutableLongSetOf is recommended over mutableStateOf Immutable/Persistent/Set<Long> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingFloatSet = """
            Using mutableFloatSetOf is recommended over mutableStateOf Immutable/Persistent/Set<Float> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        // Maps (Int -> X)
        val MutableStateAutoboxingIntIntMap = """
            Using mutableIntIntMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Int, Int> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingIntLongMap = """
            Using mutableIntLongMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Int, Long> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingIntFloatMap = """
            Using mutableIntFloatMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Int, Float> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        // Maps (Long -> X)
        val MutableStateAutoboxingLongIntMap = """
            Using mutableLongIntMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Long, Int> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingLongLongMap = """
            Using mutableLongLongMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Long, Long> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingLongFloatMap = """
            Using mutableLongFloatMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Long, Float> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        // Maps (Float -> X)
        val MutableStateAutoboxingFloatIntMap = """
            Using mutableFloatIntMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Float, Int> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingFloatLongMap = """
            Using mutableFloatLongMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Float, Long> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()

        val MutableStateAutoboxingFloatFloatMap = """
            Using mutableFloatFloatMapOf is recommended over mutableStateOf Immutable/Persistent/Map<Float, Float> due to its better performance.

            See https://mrmans0n.github.io/compose-rules/rules/#use-mutablestateof-type-specific-variants-when-possible for more information.
        """.trimIndent()
    }
}
