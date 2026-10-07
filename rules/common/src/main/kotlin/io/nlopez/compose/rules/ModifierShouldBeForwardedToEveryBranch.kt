// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.argumentsUsingModifiers
import io.nlopez.compose.core.util.hasAnyContextArguments
import io.nlopez.compose.core.util.isAnnotatedWith
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.modifierParameters
import io.nlopez.compose.core.util.modifierTypeNames
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

/** Returns true for forwarding calls, false for missing forwarding, and omits unknown calls. */
typealias BranchModifierForwardingResolver = (
    function: KtNamedFunction,
    calls: List<KtCallExpression>,
    config: ComposeKtConfig,
) -> Map<KtCallExpression, Boolean>

class ModifierShouldBeForwardedToEveryBranch(
    private val resolveForwarding: BranchModifierForwardingResolver = ::psiBranchForwarding,
) : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        val namedFunction = function as? KtNamedFunction ?: return
        val root = function.bodyExpression?.singleStatement() ?: return
        if (root !is KtIfExpression && root !is KtWhenExpression) return
        val forwarding = resolveForwarding(namedFunction, root.branchCalls(), config)
        if (true !in forwarding.values) return
        forwarding.filterValues { !it }.keys.forEach { call ->
            emitter.report(call, MissingBranchModifier, false)
        }
    }

    private fun KtExpression.singleStatement(): KtExpression? = when (this) {
        is KtBlockExpression -> statements.singleOrNull()?.singleStatement()
        is KtParenthesizedExpression -> expression?.singleStatement()
        is KtReturnExpression -> returnedExpression?.singleStatement()
        else -> this
    }

    private fun KtExpression.branchCalls(): List<KtCallExpression> = when (val expression = singleStatement()) {
        is KtIfExpression -> expression.then?.branchCalls().orEmpty() + expression.`else`?.branchCalls().orEmpty()
        is KtWhenExpression -> expression.entries.flatMap { it.expression?.branchCalls().orEmpty() }
        is KtQualifiedExpression -> listOfNotNull(expression.selectorExpression as? KtCallExpression)
        is KtCallExpression -> listOf(expression)
        else -> emptyList()
    }

    companion object {
        val MissingBranchModifier =
            "One branch forwards the modifier parameter, but this branch does not. Pass it to every branch whose composable accepts a modifier."
    }
}

private fun psiBranchForwarding(
    function: KtNamedFunction,
    calls: List<KtCallExpression>,
    config: ComposeKtConfig,
): Map<KtCallExpression, Boolean> {
    // Without resolution, limit analysis to top-level Unit functions with an identifiable modifier parameter.
    val file = function.parent as? KtFile ?: return emptyMap()
    if (function.receiverTypeReference != null || function.hasAnyContextArguments) return emptyMap()
    if (function.typeReference?.text?.let { it != "Unit" } == true) return emptyMap()
    val modifiers = function.modifierParameters(config)
    val modifier = modifiers.firstOrNull { it.name == "modifier" } ?: modifiers.singleOrNull() ?: return emptyMap()
    val modifierName = modifier.name ?: return emptyMap()
    val typeNames = modifierTypeNames(config)
    // Only unqualified, unambiguous same-file calls can be matched without semantic resolution.
    // A when subject declaration may shadow either the modifier or the callee.
    return calls.mapNotNull { call ->
        if (call.parent is KtQualifiedExpression) return@mapNotNull null
        if (call.parents.takeWhile {
                it != function
            }.any { it is KtWhenExpression && it.subjectVariable != null }
        ) {
            return@mapNotNull null
        }
        val name =
            (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return@mapNotNull null
        // Parameters and imports can hide a same-file declaration. Require a single local candidate.
        if (function.valueParameters.any { it.name == name }) return@mapNotNull null
        if (file.importDirectives.any { (it.aliasName ?: it.importedFqName?.shortName()?.asString()) == name }) {
            return@mapNotNull null
        }
        val target = file.declarations.filterIsInstance<KtNamedDeclaration>()
            .singleOrNull { it.name == name } as? KtNamedFunction ?: return@mapNotNull null
        // Only Unit composables accepting the same modifier type participate in the forwarding check.
        if (!target.isComposable || target.isAnnotatedWith(setOf("ReadOnlyComposable"))) return@mapNotNull null
        if (target.receiverTypeReference != null || target.hasAnyContextArguments) return@mapNotNull null
        if (target.typeReference?.text?.let { it != "Unit" } ?: !target.hasBlockBody()) return@mapNotNull null
        if (target.valueParameters.any { it.isVarArg }) return@mapNotNull null
        val targetModifiers = target.modifierParameters(config)
        val targetModifier = targetModifiers.firstOrNull { it.name == "modifier" }
            ?: targetModifiers.singleOrNull() ?: return@mapNotNull null
        if (targetModifier.typeReference?.text != modifier.typeReference?.text) return@mapNotNull null

        // Named arguments identify the parameter directly. Positional matching is safe only before named arguments.
        val modifierIndex = target.valueParameters.indexOf(targetModifier)
        val namedArgument = call.valueArguments.singleOrNull { it.getArgumentName()?.text == targetModifier.name }
        val positionalCandidate = call.valueArguments.getOrNull(modifierIndex)
        if (namedArgument == null && positionalCandidate?.isNamed() == false &&
            call.valueArguments.take(modifierIndex).any { it.isNamed() }
        ) {
            return@mapNotNull null
        }
        val positionalArgument = positionalCandidate?.takeIf {
            call.valueArguments.take(modifierIndex + 1).none { argument -> argument.isNamed() }
        }
        val argument = namedArgument ?: positionalArgument
        val forwards = when {
            argument == null -> false
            argument in call.argumentsUsingModifiers(setOf(modifierName), typeNames) -> true
            argument.getArgumentExpression()?.text in typeNames -> false
            else -> null // Unknown expressions and aliases are not proof of missing forwarding.
        }
        forwards?.let { call to it }
    }.toMap()
}
