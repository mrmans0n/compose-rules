// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.annotator.errors

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.configurationStore.runAsWriteActionIfNeeded
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.BaseRefactoringIntentionAction
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.modify
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter

data class ParameterOrderError(
    val composable: KtFunction,
    val currentOrder: List<KtParameter>,
    val properOrder: List<KtParameter>,
) : ComposableError(composable) {

    override val markerTag: KtElement = composable.valueParameterList ?: composable
    override val hasFix: Boolean = true

    override val severity: HighlightSeverity
        get() = HighlightSeverity.ERROR

    override val message: String
        get() = """
            Parameters for this @Composable are not in the right order.
            It is: [${currentOrder.joinToString { it.text }}]
            and should be: [${properOrder.joinToString { it.text }}]
        """.trimIndent()

    override fun fix(): IntentionAction = object : BaseRefactoringIntentionAction() {
        override fun startInWriteAction(): Boolean = false
        override fun getText(): String = "Fix @Composable parameter order"
        override fun getFamilyName(): String = "Fix @Composable parameter order"
        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean = true
        override fun getElementToMakeWritable(currentFile: PsiFile): PsiElement = currentFile

        private val configuration by lazy {
            object : KotlinChangeSignatureConfiguration {
                override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                    // Process all params and associate to their KotlinParameterInfo
                    val params = originalDescriptor.parameters.associateBy { paramInfo ->
                        properOrder.first { it.name == paramInfo.name }
                    }

                    return runAsWriteActionIfNeeded {
                        originalDescriptor.modify { methodDescriptor ->
                            // Clear all params
                            repeat(methodDescriptor.parametersCount) { methodDescriptor.removeParameter(0) }
                            // Re-add them but in the desired order
                            properOrder.mapNotNull { params[it] }.forEach { paramInfo ->
                                methodDescriptor.addParameter(paramInfo)
                            }
                        }
                    }
                }

                override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = true
            }
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            runChangeSignature(
                project = project,
                editor = editor,
                callableDescriptor = composable.unsafeResolveToDescriptor() as CallableDescriptor,
                configuration = configuration,
                defaultValueContext = composable.context!!,
            )
        }
    }
}
