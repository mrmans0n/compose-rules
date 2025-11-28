// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.ElementType
import com.intellij.psi.impl.source.tree.LeafPsiElement
import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.ifFix
import io.nlopez.compose.core.util.definedInInterface
import io.nlopez.compose.core.util.findAllChildrenByClass
import io.nlopez.compose.core.util.findDirectChildrenByClass
import io.nlopez.compose.core.util.findDirectFirstChildByClass
import io.nlopez.compose.core.util.firstChildLeafOrSelf
import io.nlopez.compose.core.util.isOverride
import io.nlopez.compose.core.util.lastChildLeafOrSelf
import io.nlopez.compose.core.util.nextCodeSibling
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.parents

class ViewModelInjection : ComposeKtVisitor {

    override fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {
        if (function.isOverride || function.definedInInterface) return

        val bodyBlock = function.bodyBlockExpression ?: return

        val knownViewModelFactories = DefaultKnownViewModelFactories +
            config.getSet("viewModelFactories", emptySet())

        bodyBlock.findAllChildrenByClass<KtProperty>()
            .flatMap { property ->
                property.findDirectChildrenByClass<KtCallExpression>()
                    .filter { it.calleeExpression?.text in knownViewModelFactories }
                    .filterNot { it.isNavigation(bodyBlock) }
                    .map { property to it.calleeExpression!!.text }
            }
            .forEach { (property, viewModelFactoryName) ->
                emitter.report(property, errorMessage(viewModelFactoryName), true)
                    .ifFix { fix(function, property, viewModelFactoryName) }
            }
    }

    private fun fix(composable: KtFunction, property: KtProperty, viewModelFactoryName: String) {
        // First of all, we want to extract the property name and all the arguments
        val variableName = property.name
        val callExpression = property.findDirectFirstChildByClass<KtCallExpression>() ?: return
        val argumentList = callExpression.valueArgumentList ?: return

        // With factories with params, we can't know for sure if they'll continue working (they might have not be accessible
        // from the composable params). Let's filter them out.
        if (callExpression.valueArguments.isNotEmpty()) return

        // We also want the ViewModel type, with two possibilities to support:
        // val viewModel : VM = viewModel(...)
        // val viewModel = viewModel<VM>(...)
        val viewModelTypeReference = property.typeReference
            ?: property.findDirectFirstChildByClass<KtCallExpression>()?.typeArguments?.singleOrNull()
            ?: return

        // Then we need to check the parameters on the FunctionNode. We want to be the last element added
        // EXCEPT in the case in which there is a function as the last parameter, in which case we want to be
        // second to last
        val rawViewModelType = viewModelTypeReference.text
        val rawArgumentList = argumentList.text
        val lastParameters = composable.valueParameters.takeLast(2)
        val parameterList = composable.valueParameterList ?: return

        // Generate the VALUE_PARAMETER for variableName: VMType = viewModel(...)
        val newCode = "$variableName: $rawViewModelType = $viewModelFactoryName$rawArgumentList"
        val factory = KtPsiFactory.contextual(parameterList)
        val newParam = factory.createParameter(newCode)

        when {
            // If there are no parameters, we will insert the code directly
            lastParameters.isEmpty() -> {
                // Ideally this should be:
                //  parameterList.addParameter(newParam)
                // but since Kotlin 1.9 we can't use these methods without crashing.
                (parameterList.node.lastChildLeafOrSelf() as LeafPsiElement)
                    .rawReplaceWithText("${newParam.text})")
            }

            // If the last element is a function, we need to preserve the trailing lambda, so we will insert
            // the code before that last param
            lastParameters.last().typeReference?.typeElement is KtFunctionType -> {
                // If there's only 1 param, we insert the code with the initial parenthesis
                if (lastParameters.size == 1) {
                    val firstToken = parameterList.node.firstChildLeafOrSelf() as LeafPsiElement
                    firstToken.rawReplaceWithText("($newCode, ")
                } else {
                    // If there were 2+ params, we insert the code between the two parameters
                    val lastToken = lastParameters.first()
                        .node
                        .nextCodeSibling()!!
                        .lastChildLeafOrSelf() as LeafPsiElement
                    // Last token here would be the previous comma, if there were spaces between the comma
                    // and the functional type (the next sibling), we would insert ourselves at the left of it.
                    lastToken.rawReplaceWithText("${lastToken.text} $newCode,")
                }
            }

            // Add as the last parameter
            else -> {
                // Ideally this should just be:
                //  parameterList.addParameter(newParam)
                // but since Kotlin 1.9 we can't use these methods without crashing.

                val hasTrailingComma = composable.valueParameters.last().node.nextCodeSibling()?.text == ","
                // If it has a trailing comma, no need to add a new one
                val preCommaIfNeeded = if (hasTrailingComma) "" else ","
                // And if it has a trailing comma, we'll need to preserve that in the style
                val trailingCommaIfNeeded = if (hasTrailingComma) "," else ""
                (parameterList.node.lastChildLeafOrSelf() as LeafPsiElement)
                    .rawReplaceWithText("$preCommaIfNeeded${newParam.text}$trailingCommaIfNeeded)")
            }
        }

        // And finally, we can delete the original property from the code
        // 1. If there's whitespace before (code indent spaces) we remove them
        property.node.treePrev?.takeIf { it.elementType == ElementType.WHITE_SPACE }?.psi?.delete()
        // 2. Remove the actual code
        property.delete()
    }

    private fun KtCallExpression.isNavigation(stopAt: PsiElement): Boolean = parents
        .takeWhile { it != stopAt }
        .filterIsInstance<KtCallExpression>()
        .any { it.calleeExpression?.text in KnownNavigationCallExpressions }

    companion object {
        private val KnownNavigationCallExpressions by lazy {
            setOf(
                // androidx navigation
                "composable",
                "NavHost",
            )
        }

        private val DefaultKnownViewModelFactories by lazy {
            setOf(
                // AAC VM
                "viewModel",
                // Weaver (Twitter in-house)
                "weaverViewModel",
                // Hilt
                "hiltViewModel",
                // Whetstone
                "injectedViewModel",
                // Mavericks
                "mavericksViewModel",
                // Tangle (Anvil extensions)
                "tangleViewModel",
                // Metro (expected name)
                "metroViewModel",
                // Anvil (expected name)
                "anvilViewModel",
            )
        }

        fun errorMessage(factoryName: String) = """
            Implicit dependencies of composables should be made explicit.
            Usages of $factoryName to acquire a ViewModel should be done in composable default parameters, so that it is more testable and flexible.
            See https://mrmans0n.github.io/compose-rules/rules/#viewmodels for more information.
        """.trimIndent()
    }
}
