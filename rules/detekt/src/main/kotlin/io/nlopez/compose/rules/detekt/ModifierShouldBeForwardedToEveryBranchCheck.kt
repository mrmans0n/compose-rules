// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(KaExperimentalApi::class)

package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.ModifierShouldBeForwardedToEveryBranch
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.resolveCall
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.net.URI

class ModifierShouldBeForwardedToEveryBranchCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Forward modifiers to every applicable branch",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#forward-modifiers-to-every-applicable-branch"),
    ),
    ComposeKtVisitor by ModifierShouldBeForwardedToEveryBranch(::resolvedBranchForwarding),
    RequiresAnalysisApi

private fun resolvedBranchForwarding(
    function: KtNamedFunction,
    calls: List<KtCallExpression>,
    config: ComposeKtConfig,
): Map<KtCallExpression, Boolean> = runCatching {
    // Keep symbols and argument mappings within one analysis session; return only PSI calls and forwarding facts.
    analyze(function) {
        val enclosing = function.symbol
        val returnType = enclosing.returnType.fullyExpandedType
        if (returnType.isMarkedNullable || (returnType as? KaClassType)?.classId != StandardClassIds.Unit) {
            return@analyze emptyMap()
        }
        // Match expanded types so aliases work, and inspect value parameters rather than context receivers.
        val customTypes = config.getSet("customModifiers", emptySet())
        val modifiers = enclosing.valueParameters.filter { parameter ->
            val classId = (parameter.returnType.fullyExpandedType as? KaClassType)?.classId
                ?: return@filter false
            val typeName = classId.asSingleFqName().asString()
            typeName in setOf("androidx.compose.ui.Modifier", "androidx.glance.GlanceModifier") ||
                typeName in customTypes || classId.shortClassName.asString() in customTypes
        }
        val modifier = modifiers.firstOrNull { it.name.asString() == "modifier" }
            ?: modifiers.singleOrNull() ?: return@analyze emptyMap()
        val modifierClassId = (modifier.returnType.fullyExpandedType as? KaClassType)?.classId
            ?: return@analyze emptyMap()

        // Follow resolved modifier receivers, but do not infer forwarding through arbitrary factories or aliases.
        fun forwarding(value: KtExpression): Boolean? = when (val argument = value.unwrapArgumentExpression()) {
            is KtNameReferenceExpression -> {
                // Symbol identity distinguishes the enclosing modifier from a same-named value in a branch.
                val symbol = argument.mainReference.resolveToSymbol()
                val classId = (symbol as? KaClassLikeSymbol)?.classId
                when {
                    symbol == modifier -> true

                    classId == modifierClassId -> false

                    classId?.outerClassId == modifierClassId && classId.shortClassName.asString() == "Companion" ->
                        false

                    else -> null
                }
            }

            is KtDotQualifiedExpression -> {
                val selector = argument.selectorExpression
                if (selector is KtNameReferenceExpression) {
                    forwarding(selector)
                } else {
                    val chainCall = (argument.selectorExpression as? KtCallExpression)?.resolveCall()
                    val symbol = chainCall?.signature?.symbol
                    val receiverClassId =
                        (symbol?.receiverParameter?.returnType?.fullyExpandedType as? KaClassType)?.classId
                    if ((chainCall?.signature?.returnType?.fullyExpandedType as? KaClassType)?.classId !=
                        modifierClassId ||
                        (receiverClassId != modifierClassId && symbol?.callableId?.classId != modifierClassId)
                    ) {
                        null
                    } else {
                        val receiver = forwarding(argument.receiverExpression)
                        // then can carry the enclosing modifier through its argument even with a fresh receiver.
                        val appended = if (symbol.callableId?.callableName?.asString() == "then") {
                            chainCall.valueArgumentMapping.keys.singleOrNull()?.let { forwarding(it) }
                        } else {
                            false
                        }
                        when {
                            receiver == true || appended == true -> true
                            receiver == false && appended == false -> false
                            else -> null
                        }
                    }
                }
            }

            else -> null
        }

        // Resolve the actual overload and ignore calls that do not emit modifier-accepting content.
        calls.mapNotNull { call ->
            val resolved = call.resolveCall() ?: return@mapNotNull null
            val target = resolved.signature.symbol
            val isComposable = target.hasComposableAnnotation() ||
                call.calleeExpression?.expressionType?.hasComposableAnnotation() == true
            if (!isComposable || target.hasReadOnlyComposableAnnotation()) {
                return@mapNotNull null
            }
            val targetReturnType = resolved.signature.returnType.fullyExpandedType
            if (targetReturnType.isMarkedNullable ||
                (targetReturnType as? KaClassType)?.classId != StandardClassIds.Unit
            ) {
                return@mapNotNull null
            }
            val targetModifiers = resolved.signature.valueParameters.filter { parameter ->
                (parameter.returnType.fullyExpandedType as? KaClassType)?.classId == modifierClassId
            }
            val targetModifier = targetModifiers.firstOrNull { it.symbol.name.asString() == "modifier" }
                ?: targetModifiers.singleOrNull() ?: return@mapNotNull null
            // Use the compiler's argument mapping for named, positional, and mixed argument lists.
            val argument = resolved.valueArgumentMapping.entries
                .singleOrNull { (_, parameter) -> parameter.symbol == targetModifier.symbol }?.key
            val forwards = if (argument == null) false else forwarding(argument)
            forwards?.let { call to it }
        }.toMap()
    }
}.getOrDefault(emptyMap())
