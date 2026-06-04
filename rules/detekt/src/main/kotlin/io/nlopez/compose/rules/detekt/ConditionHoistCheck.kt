// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.expectedType
import org.jetbrains.kotlin.analysis.api.components.functionType
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.siblings
import java.net.URI

/**
 * Reports Compose layouts whose only content is a conditional composable child.
 */
class ConditionHoistCheck(config: Config) :
    Rule(
        config,
        "Compose layouts with a single conditional expression should hoist the condition.",
        URI("https://mrmans0n.github.io/compose-rules/rules/#hoist-single-conditional-layout-content"),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("ConditionHoist")

    private val ignoreCallsWithArgumentNames by config(
        defaultValue = listOf(
            "modifier",
            "contentAlignment",
            "horizontalArrangement",
            "verticalAlignment",
        ),
    )

    private val ignoreCallsWithArgumentNamesSet by lazy { ignoreCallsWithArgumentNames.toSet() }

    private val contentEmitters by config(defaultValue = emptyList<String>())

    private val contentEmittersSet by lazy { DefaultContentEmitters + contentEmitters }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isComposable()) return

        checkBody(function.bodyExpression)
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        super.visitPropertyAccessor(accessor)
        if (!accessor.isGetter) return
        if (!accessor.isComposable()) return

        checkBody(accessor.bodyExpression)
    }

    private fun checkBody(body: KtExpression?) {
        body ?: return
        val candidates = body.composableCallsInCurrentDeclaration()
            .asSequence()
            .filter { call -> call.isConfiguredContentEmitter() }
            .filterNot { call -> call.hasExplicitArgumentMappedToAny(ignoreCallsWithArgumentNamesSet) }
            .filterNot { call -> call.hasExplicitNonLambdaArguments() }
            .mapNotNull { call -> call.singleComposableLambdaArgument() }
            .filterNot { lambda -> lambda.hasValueParameters() }
            .mapNotNull { lambda -> lambda.singleConditionalComposableChild() }
            .distinct()

        candidates.forEach(::reportConditionCouldBeHoisted)
    }

    private fun KtCallExpression.singleComposableLambdaArgument(): KtLambdaExpression? {
        val composableLambdas = directLambdaArguments().filter { lambda -> lambda.isComposable() }
        return composableLambdas.singleOrNull()
    }

    private fun KtCallExpression.directLambdaArguments(): List<KtLambdaExpression> = (
        valueArguments.mapNotNull { argument -> argument.getArgumentExpression() as? KtLambdaExpression } +
            lambdaArguments.mapNotNull { argument -> argument.getLambdaExpression() }
        ).distinct()

    private fun KtCallExpression.hasExplicitNonLambdaArguments(): Boolean =
        valueArguments.any { argument -> argument.getArgumentExpression() !is KtLambdaExpression }

    private fun KtCallExpression.isConfiguredContentEmitter(): Boolean = calleeExpression?.text in contentEmittersSet

    private fun KtLambdaExpression.hasValueParameters(): Boolean = valueParameters.isNotEmpty() ||
        runCatching {
            analyze(this) {
                (functionLiteral.functionType as? KaFunctionType)?.parameterTypes?.isNotEmpty() == true ||
                    (expectedType as? KaFunctionType)?.parameterTypes?.isNotEmpty() == true
            }
        }.getOrDefault(false)

    private fun KtLambdaExpression.singleConditionalComposableChild(): KtIfExpression? {
        val body = bodyExpression ?: return null
        val ifExpression = body.directIfExpressions().singleOrNull() ?: return null
        if (ifExpression.then?.hasEmittedComposableCall() != true) return null
        if (ifExpression.`else`?.hasEmittedComposableCall() == true) return null
        if (ifExpression.hasContentSiblings()) return null
        if (ifExpression.condition?.usesImplicitReceiverOf(this) == true) return null
        return ifExpression
    }

    private fun KtExpression.usesImplicitReceiverOf(lambda: KtLambdaExpression): Boolean = runCatching {
        if (!lambda.hasFunctionTypeReceiver()) return@runCatching false

        analyze(this) {
            val receiverTypes = listOfNotNull(
                (lambda.functionLiteral.functionType as? KaFunctionType)?.receiverType,
                (lambda.expectedType as? KaFunctionType)?.receiverType,
            )
            val receiverClasses = receiverTypes.mapNotNull { type -> type.symbol?.sourcePsiSafe<KtClassOrObject>() }
            val receiverClassIds = receiverTypes.mapNotNull { type -> type.symbol?.classId }.toSet()
            if (receiverClasses.isEmpty() && receiverClassIds.isEmpty()) return@analyze false

            simpleNameExpressions().any { expression ->
                val receiverMember = when (val symbol = expression.mainReference.resolveToSymbol()) {
                    is KaPropertySymbol -> symbol.toReceiverMember(
                        sourceClass = symbol.sourcePsiSafe<KtProperty>()
                            ?.getStrictParentOfType<KtClassOrObject>(),
                    )

                    is KaFunctionSymbol -> symbol.toReceiverMember(
                        sourceClass = symbol.sourcePsiSafe<KtNamedFunction>()
                            ?.getStrictParentOfType<KtClassOrObject>(),
                    )

                    else -> ReceiverMember()
                }

                receiverMember.sourceClass in receiverClasses ||
                    receiverMember.classId in receiverClassIds ||
                    receiverMember.extensionReceiverSourceClass in receiverClasses ||
                    receiverMember.extensionReceiverClassId in receiverClassIds
            }
        }
    }.getOrDefault(false)

    private fun KaCallableSymbol.toReceiverMember(sourceClass: KtClassOrObject?): ReceiverMember = ReceiverMember(
        sourceClass = sourceClass,
        classId = callableId?.classId,
        extensionReceiverSourceClass = receiverParameter?.returnType?.symbol?.sourcePsiSafe<KtClassOrObject>(),
        extensionReceiverClassId = receiverParameter?.returnType?.symbol?.classId,
    )

    private fun KtExpression.simpleNameExpressions(): List<KtSimpleNameExpression> =
        listOfNotNull(this as? KtSimpleNameExpression) + collectDescendantsOfType()

    private fun KtElement.directIfExpressions(): List<KtIfExpression> = when (this) {
        is KtIfExpression -> listOf(this)
        is KtBlockExpression -> statements.filterIsInstance<KtIfExpression>()
        else -> emptyList()
    }

    private data class ReceiverMember(
        val sourceClass: KtClassOrObject? = null,
        val classId: ClassId? = null,
        val extensionReceiverSourceClass: KtClassOrObject? = null,
        val extensionReceiverClassId: ClassId? = null,
    )

    private fun KtIfExpression.hasContentSiblings(): Boolean {
        val before = siblings(forward = false, withItself = false)
        val after = siblings(forward = true, withItself = false)
        return (before + after)
            .filterIsInstance<KtElement>()
            .any()
    }

    private fun KtElement.hasEmittedComposableCall(): Boolean {
        if ((this as? KtCallExpression)?.isComposableCall() == true) return true

        var hasEmittedComposableCall = false
        accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.isComposableCall()) {
                        hasEmittedComposableCall = true
                        return
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                    // Deferred lambda bodies are not emitted by this branch.
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    // Nested function bodies are not emitted by this branch.
                }

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                    // Nested accessor bodies are not emitted by this branch.
                }

                override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                    // Member bodies are not emitted by this branch.
                }
            },
        )
        return hasEmittedComposableCall
    }

    private fun KtElement.composableCallsInCurrentDeclaration(): List<KtCallExpression> {
        val calls = mutableListOf<KtCallExpression>()
        accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.isComposableCall()) {
                        calls += expression
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    // Nested function bodies are visited as their own declarations.
                }

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                    // Nested accessor bodies are visited as their own declarations.
                }

                override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                    // Member bodies are not part of the current composable declaration.
                }
            },
        )
        return calls
    }

    private fun reportConditionCouldBeHoisted(ifExpression: KtIfExpression) {
        report(
            Finding(
                Entity.from(ifExpression),
                ConditionCouldBeHoisted,
            ),
        )
    }

    internal companion object {
        private val DefaultContentEmitters = setOf(
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
            "AssistChip",
            "DatePickerDialog",
            "DockedSearchBar",
            "ElevatedAssistChip",
            "ElevatedFilterChip",
            "ElevatedSuggestionChip",
            "FilterChip",
            "HorizontalDivider",
            "InputChip",
            "InputField",
            "ModalBottomSheet",
            "NavigationBar",
            "NavigationBarItem",
            "PlainTooltip",
            "RichTooltip",
            "SearchBar",
            "SuggestionChip",
            "VerticalDivider",
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

        val ConditionCouldBeHoisted = """
            Conditional expression could be hoisted out of the layout.

            See https://mrmans0n.github.io/compose-rules/rules/#hoist-single-conditional-layout-content for more information.
        """.trimIndent()
    }
}
