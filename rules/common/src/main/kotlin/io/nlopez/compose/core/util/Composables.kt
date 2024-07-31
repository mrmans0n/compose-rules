// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core.util

import io.nlopez.compose.core.ComposeKtConfig
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import kotlin.math.max

private tailrec suspend fun SequenceScope<KtCallExpression>.scan(elements: List<PsiElement>) {
    if (elements.isEmpty()) return
    return scan(
        elements
            .mapNotNull { current ->
                if (current is KtCallExpression) {
                    if (current.calleeExpression?.text in ComposableNonEmittersList) {
                        null
                    } else {
                        current.also { yield(it) }
                    }
                } else {
                    current
                }
            }
            .flatMap { it.children.toList() },
    )
}

context(ComposeKtConfig)
val KtFunction.emitsContent: Boolean
    get() = if (isComposable) sequence { scan(listOf(this@emitsContent)) }.any { it.emitsContent } else false

context(ComposeKtConfig)
val KtCallExpression.emitsContent: Boolean
    get() {
        val methodName = calleeExpression?.text ?: return false

        // If in non emitters list, we assume it doesn't emit content
        if (isInContentEmittersDenylist) return false

        return methodName in ComposableEmittersList + getSet("contentEmitters", emptySet()) ||
            containsComposablesWithModifiers
    }

context(ComposeKtConfig)
val KtCallExpression.isInContentEmittersDenylist: Boolean
    get() {
        val methodName = calleeExpression?.text ?: return false

        // If in non emitters list, we assume it doesn't emit content
        if (methodName in ComposableNonEmittersList) return true

        // If in denylist, we will assume it doesn't emit content (regardless of anything else).
        if (methodName in getSet("contentEmittersDenylist", emptySet())) return true
        return false
    }

private val KtCallExpression.containsComposablesWithModifiers: Boolean
    get() {
        // Check if there is a "modifier" applied
        val hasNamedModifier = valueArguments
            .filter { it.isNamed() }
            .any { it.getArgumentName()?.text == "modifier" }

        if (hasNamedModifier) return true

        // Check if there is any Modifier chain (e.g. `Modifier.fillMaxWidth()`)
        return valueArguments.mapNotNull { it.getArgumentExpression() }
            .filterIsInstance<KtDotQualifiedExpression>()
            .any { it.rootExpression.text == "Modifier" }
    }

context(ComposeKtConfig)
private fun KtExpression.uiEmitterCount(): Int = when (val current = this) {
    // Something like a function body or a var declaration. E.g. @Composable fun A() { Text("bleh") }
    is KtDeclarationWithBody -> current.bodyBlockExpression?.uiEmitterCount() ?: 0

    // A whole code block. E.g. { Text("bleh") Text("meh") }
    is KtBlockExpression -> {
        current.statements.fold(0) { acc, next -> acc + next.uiEmitterCount() }
    }

    is KtCallExpression -> when {
        // Direct statements. E.g. Text("bleh")
        current.emitsContent -> 1
        // Scoped functions like run, with, etc.
        current.calleeExpression?.text in KotlinScopeFunctions ->
            current.lambdaArguments.singleOrNull()?.getLambdaExpression()?.bodyExpression?.uiEmitterCount() ?: 0

        else -> 0
    }

    // for loops. E.g. for (item in list) { Text(item) }
    is KtForExpression -> {
        // Assume at least 2 iterations if any, as we can't know how many there will be.
        current.body?.uiEmitterCount()?.takeIf { it > 0 }?.let { it + 1 } ?: 0
    }

    // Scoped function statements. E.g. text?.let { Text(it) }
    is KtSafeQualifiedExpression -> {
        (current.selectorExpression as? KtCallExpression)
            ?.takeIf { it.calleeExpression?.text in KotlinScopeFunctions }
            ?.lambdaArguments
            ?.singleOrNull()
            ?.getLambdaExpression()
            ?.bodyExpression
            ?.uiEmitterCount()
            ?: 0
    }

    // Elvis operators. E.g. text?.let { Text(it) } ?: Text("default")
    is KtBinaryExpression -> {
        if (current.operationToken == KtTokens.ELVIS) {
            val leftCount = current.left?.uiEmitterCount() ?: 0
            val rightCount = current.right?.uiEmitterCount() ?: 0
            max(leftCount, rightCount)
        } else {
            0
        }
    }

    is KtIfExpression -> {
        val ifCount = current.then?.uiEmitterCount() ?: 0
        val elseCount = current.`else`?.uiEmitterCount() ?: 0
        max(ifCount, elseCount)
    }

    else -> 0
}

context(ComposeKtConfig)
private fun KtFunction.indirectUiEmitterCount(mapping: Map<KtFunction, Int>): Int {
    val bodyBlock = bodyBlockExpression ?: return 0
    return bodyBlock.statements
        .filterIsInstance<KtCallExpression>()
        .count { callExpression ->
            // If it's a direct hit on our list, it should count directly
            if (callExpression.emitsContent) return@count true

            val name = callExpression.calleeExpression?.text ?: return@count false
            // If the hit is in the provided mapping, it means it is using a composable that we know emits UI,
            // that we inferred from previous passes
            val value = mapping.mapKeys { entry -> entry.key.name }[name] ?: return@count false
            value > 0
        }
}

context(ComposeKtConfig)
fun Sequence<KtFunction>.createDirectComposableToEmissionCountMapping(): Map<KtFunction, Int> =
    associateWith { it.uiEmitterCount() }

context(ComposeKtConfig)
fun refineComposableToEmissionCountMapping(
    initialMapping: Map<KtFunction, Int>,
): Map<KtFunction, Int> {
    var current = initialMapping

    var shouldMakeAnotherPass = true
    while (shouldMakeAnotherPass) {
        val updatedMapping = current.mapValues { (functionNode, _) ->
            functionNode.indirectUiEmitterCount(current)
        }
        when {
            updatedMapping != current -> current = updatedMapping
            else -> shouldMakeAnotherPass = false
        }
    }

    return current
}

/**
 * This is a denylist with common composables that emit content in their own window. Feel free to add more elements
 * if you stumble upon false positives that should not have triggered an error from this rule, and are in foundational
 * libraries.
 */
private val ComposableNonEmittersList by lazy {
    setOf(
        "AlertDialog",
        "DatePickerDialog",
        "Dialog",
        "ModalBottomSheetLayout",
        "ModalBottomSheet",
        "Popup",
    )
}

/**
 * This is an allowlist with common composables that emit content. Feel free to add more elements if you stumble
 * upon them in code reviews that should have triggered an error from this rule.
 */
private val ComposableEmittersList by lazy {
    setOf(
        // androidx.compose.foundation
        "BasicTextField",
        "Box",
        "Canvas",
        "ClickableText",
        "Column",
        "Icon",
        "Image",
        "Layout",
        "LazyColumn",
        "LazyRow",
        "LazyVerticalGrid",
        "Row",
        "Spacer",
        "Text",
        // android.compose.material
        "BottomDrawer",
        "Button",
        "Card",
        "Checkbox",
        "CircularProgressIndicator",
        "Divider",
        "DropdownMenu",
        "DropdownMenuItem",
        "ExposedDropdownMenuBox",
        "ExtendedFloatingActionButton",
        "FloatingActionButton",
        "IconButton",
        "IconToggleButton",
        "LeadingIconTab",
        "LinearProgressIndicator",
        "ListItem",
        "ModalBottomSheetLayout",
        "ModalDrawer",
        "NavigationRail",
        "NavigationRailItem",
        "OutlinedButton",
        "OutlinedTextField",
        "RadioButton",
        "Scaffold",
        "ScrollableTabRow",
        "Slider",
        "SnackbarHost",
        "Surface",
        "SwipeToDismiss",
        "Switch",
        "Tab",
        "TabRow",
        "TextButton",
        "TopAppBar",
        // androidx.compose.material3 (there are some dupes with M2 names so only adding the new ones)
        "DatePickerDialog",
        "DockedSearchBar",
        "ExposedDropdownMenuBox",
        "InputField",
        "ModalBottomSheet",
        "PlainTooltip",
        "RichTooltip",
        "SearchBar",
        // Accompanist
        "BottomNavigation",
        "BottomNavigationContent",
        "BottomNavigationSurface",
        "FlowColumn",
        "FlowRow",
        "HorizontalPager",
        "HorizontalPagerIndicator",
        "SwipeRefresh",
        "SwipeRefreshIndicator",
        "TopAppBarContent",
        "TopAppBarSurface",
        "VerticalPager",
        "VerticalPagerIndicator",
        "WebView",
    )
}

val KtProperty.declaresCompositionLocal: Boolean
    get() = !isVar &&
        hasInitializer() &&
        initializer is KtCallExpression &&
        (initializer as KtCallExpression).referenceExpression()?.text in CompositionLocalReferenceExpressions

private val CompositionLocalReferenceExpressions by lazy {
    setOf(
        "staticCompositionLocalOf",
        "compositionLocalOf",
    )
}

val KtCallExpression.isRestartableEffect: Boolean
    get() = calleeExpression?.text in RestartableEffects

// From https://developer.android.com/jetpack/compose/side-effects#restarting-effects
private val RestartableEffects by lazy {
    setOf(
        "LaunchedEffect",
        "produceState",
        "DisposableEffect",
        "produceRetainedState", // Circuit
    )
}

fun KtCallExpression.isRemembered(stopAt: PsiElement): Boolean = parents
    .takeWhile { it != stopAt }
    .filterIsInstance<KtCallExpression>()
    .any { it.calleeExpression?.text?.startsWith("remember") == true }
