// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.getLambdaArgumentName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getParentResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI
import java.util.Deque
import java.util.LinkedList

val KtAnnotated.isComposable: Boolean
    get() = annotationEntries.any { it.typeReference?.fqNameString == COMPOSABLE }

val KtTypeReference.asKotlinType: KotlinType?
    get() = analyze(BodyResolveMode.PARTIAL).get(BindingContext.TYPE, this)

val KtTypeReference.fqNameString: String?
    get() = asKotlinType?.fqName?.asString()

val KtLambdaArgument.isComposable: Boolean
    get() {
        // We will first obtain the parameter name via IJ resolution
        val context = analyze(BodyResolveMode.PARTIAL)
        val parameterName = getLambdaArgumentName(context) ?: return false

        // Now we want to obtain the function that represents the destination composable
        val destination = getParentResolvedCall(context)?.resultingDescriptor ?: return false

        // And now that we have the destination, we look in its parameters for the one matching the name,
        // and return whether it is a composable
        return destination.valueParameters
            .filter { it.name == parameterName }
            .any { it.annotations.hasAnnotation(FqName(COMPOSABLE)) }
    }

@OptIn(IDEAPluginsCompatibilityAPI::class)
val KtCallExpression.isComposableDestination: Boolean
    get() = getResolvedCall(analyze(BodyResolveMode.PARTIAL))
        ?.resultingDescriptor
        ?.annotations
        ?.hasAnnotation(FqName(COMPOSABLE)) == true

@OptIn(IDEAPluginsCompatibilityAPI::class)
val KtCallExpression.isComposeRuntime: Boolean
    get() = getResolvedCall(analyze(BodyResolveMode.PARTIAL))
        ?.resultingDescriptor
        ?.fqNameSafe
        ?.startsWith(ANDROIDX_COMPOSE_RUNTIME_NAME) == true

val KtFunction.hasComposeModifierParam: Boolean
    get() = valueParameters.any { it.typeReference?.fqNameString == MODIFIER }

val ANDROIDX_COMPOSE_RUNTIME_NAME = Name.identifier("androidx.compose.runtime")

const val COMPOSABLE = "androidx.compose.runtime.Composable"
const val MODIFIER = "androidx.compose.ui.Modifier"

fun <T> T.runIf(condition: Boolean, action: T.() -> T) = if (condition) action() else this

inline fun <reified T : PsiElement> PsiElement.findAllChildrenByType(): Sequence<T> = sequence {
    val queue: Deque<PsiElement> = LinkedList()
    queue.add(this@findAllChildrenByType)
    while (queue.isNotEmpty()) {
        val current = queue.pop()
        if (current is T) {
            yield(current)
        }
        queue.addAll(current.children)
    }
}
