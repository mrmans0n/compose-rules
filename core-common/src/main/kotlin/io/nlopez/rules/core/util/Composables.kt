// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import io.nlopez.rules.core.ComposeKtConfig.Companion.config
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

val KtFunction.emitsContent: Boolean
    get() {
        return if (isComposable) {
            sequence {
                tailrec suspend fun SequenceScope<KtCallExpression>.scan(elements: List<PsiElement>) {
                    if (elements.isEmpty()) return
                    val toProcess = elements
                        .mapNotNull { current ->
                            if (current is KtCallExpression) {
                                if (current.emitExplicitlyNoContent) {
                                    null
                                } else {
                                    yield(current)
                                    current
                                }
                            } else {
                                current
                            }
                        }
                        .flatMap { it.children.toList() }
                    return scan(toProcess)
                }
                scan(listOf(this@emitsContent))
            }.any { it.emitsContent }
        } else {
            false
        }
    }

private val KtCallExpression.emitExplicitlyNoContent: Boolean
    get() = calleeExpression?.text in ComposableNonEmittersList

val KtCallExpression.emitsContent: Boolean
    get() {
        val methodName = calleeExpression?.text ?: return false
        return methodName in ComposableEmittersList ||
            ComposableEmittersListRegex.matches(methodName) ||
            methodName in config().getSet("contentEmitters", emptySet()) ||
            containsComposablesWithModifiers
    }

private val KtCallExpression.containsComposablesWithModifiers: Boolean
    get() = valueArguments
        .filter { it.isNamed() }
        .any { it.getArgumentName()?.text == "modifier" }

/**
 * This is a denylist with common composables that emit content in their own window. Feel free to add more elements
 * if you stumble upon them in code reviews that should have triggered an error from this rule.
 */
private val ComposableNonEmittersList = setOf(
    "AlertDialog",
    "ModalBottomSheetLayout",
)

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

val ComposableEmittersListRegex by lazy {
    Regex(
        listOf(
            "Spacer\\d*", // Spacer() + SpacerNUM()
        ).joinToString(
            separator = "|",
            prefix = "(",
            postfix = ")",
        ),
    )
}

val KtProperty.declaresCompositionLocal: Boolean
    get() = !isVar &&
        hasInitializer() &&
        initializer is KtCallExpression &&
        (initializer as KtCallExpression).referenceExpression()?.text in CompositionLocalReferenceExpressions

private val CompositionLocalReferenceExpressions by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        "staticCompositionLocalOf",
        "compositionLocalOf",
    )
}

val KtCallExpression.isRestartableEffect: Boolean
    get() = calleeExpression?.text in RestartableEffects

// From https://developer.android.com/jetpack/compose/side-effects#restarting-effects
private val RestartableEffects by lazy(LazyThreadSafetyMode.NONE) {
    setOf(
        "LaunchedEffect",
        "produceState",
        "DisposableEffect",
    )
}
