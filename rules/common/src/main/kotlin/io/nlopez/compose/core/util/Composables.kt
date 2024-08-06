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
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
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
private fun KtExpression.uiEmitterCount(): Int {
    // For early return false positives detection we need to know where we are at. But yea, yikes.
    var totalEmittersFound = 0
    var currentBlockStartedAt = 0

    // Function for actually counting. As we can't know how big the code would be, let's be careful wrt recursion.
    val emitterCount = DeepRecursiveFunction<KtExpression?, Int> { current ->
        when (current) {
            null -> 0

            // Something like a function body or a var declaration. E.g. @Composable fun A() { Text("bleh") }
            is KtDeclarationWithBody -> callRecursive(current.bodyBlockExpression)

            // A whole code block. E.g. { Text("bleh") Text("meh") }
            is KtBlockExpression -> {
                currentBlockStartedAt = totalEmittersFound
                current.statements.fold(0) { acc, next -> acc + callRecursive(next) }
            }

            is KtCallExpression -> when {
                // Direct statements. E.g. Text("bleh")
                current.emitsContent -> {
                    totalEmittersFound++
                    1
                }
                // Scoped functions like run, with, etc.
                current.calleeExpression?.text in KotlinScopeFunctions ->
                    callRecursive(current.lambdaArguments.singleOrNull()?.getLambdaExpression()?.bodyExpression)

                else -> 0
            }

            // for loops, while loops, do while loops, etc. E.g. for (item in list) { Text(item) }
            is KtLoopExpression -> {
                // Assume at the very least 2 iterations (found 1..n, and then +1, so it'll be 2..n),
                // as we can't know how many there will be.
                callRecursive(current.body).takeIf { it > 0 }?.let { emitters ->
                    // Need to do this to take the +1 into account.
                    totalEmittersFound++
                    emitters + 1
                } ?: 0
            }

            // Scoped function statements. E.g. text?.let { Text(it) }
            is KtSafeQualifiedExpression -> {
                callRecursive(
                    (current.selectorExpression as? KtCallExpression)
                        ?.takeIf { it.calleeExpression?.text in KotlinScopeFunctions }
                        ?.lambdaArguments
                        ?.singleOrNull()
                        ?.getLambdaExpression()
                        ?.bodyExpression,
                )
            }

            // Elvis operators. E.g. text?.let { Text(it) } ?: Text("default")
            is KtBinaryExpression -> {
                if (current.operationToken == KtTokens.ELVIS) {
                    val leftCount = callRecursive(current.left)
                    val rightCount = callRecursive(current.right)
                    max(leftCount, rightCount)
                } else {
                    0
                }
            }

            // Conditionals. E.g. if (something) { Text("bleh") } else { Test("meh") }
            is KtIfExpression -> {
                val ifCount = callRecursive(current.then)
                val elseCount = callRecursive(current.`else`)
                max(ifCount, elseCount)
            }

            // When expressions.
            is KtWhenExpression -> {
                current.entries.maxOfOrNull { callRecursive(it.expression) } ?: 0
            }

            is KtReturnExpression -> {
                // Ignore labeled expressions (E.g. return@bleh)
                val functionReturn = current.labeledExpression == null
                if (functionReturn) {
                    // This is nasty, but it's simple. We want to exclude early returns from the count,
                    // we'll need to subtract 1 in the case where the code block with the return started without
                    // any emitter, and only 1 was found before the return, so we return -1 to cancel it out.
                    //
                    // For example, to avoid false positives in a code like this:
                    //   @Composable fun A() {
                    //      if (x) {
                    //          Text("1")
                    //          return
                    //      }
                    //      Text("2")
                    //   }
                    val currentBlock = totalEmittersFound - currentBlockStartedAt
                    if (currentBlock == 1 && currentBlockStartedAt == 0) -1 else 0
                } else {
                    0
                }
            }

            else -> 0
        }
    }

    return max(emitterCount(this), 0)
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
