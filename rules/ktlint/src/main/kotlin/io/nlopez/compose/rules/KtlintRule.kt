// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiNameIdentifierOwner
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfigProperty
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.startOffsetFromName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class KtlintRule(id: String, editorConfigProperties: Set<EditorConfigProperty<*>> = emptySet()) :
    RuleV2(
        ruleId = RuleId(id),
        about = About(
            maintainer = "Compose Rules",
            repositoryUrl = "https://github.com/mrmans0n/compose-rules",
            issueTrackerUrl = "https://github.com/mrmans0n/compose-rules/issues",
        ),
        usesEditorConfigProperties = editorConfigProperties,
    ),
    ComposeKtVisitor {

    private lateinit var properties: EditorConfig

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        properties = editorConfig
    }

    private val config: KtlintComposeKtConfig by lazy { KtlintComposeKtConfig(properties, usesEditorConfigProperties) }

    override fun beforeVisitChildNodes(node: ASTNode) {
        when (val psi = node.psi) {
            is KtFile -> visitFile(psi, emitter, config)

            is KtClassOrObject -> {
                visitClassOrObject(psi, emitter, config)
                if (psi is KtClass) {
                    visitClass(psi, emitter, config)
                }
            }

            is KtFunction -> {
                visitFunction(psi, emitter, config)
                if (psi.isComposable) {
                    visitComposable(psi, emitter, config)
                }
            }
        }
    }

    private val emitter = Emitter { element, errorMessage, autoCorrect ->
        val offset = if (element is PsiNameIdentifierOwner) {
            element.startOffsetFromName
        } else {
            element.startOffset
        }
        if (autoCorrect == null) {
            emit(offset, errorMessage)
        } else {
            emit(offset, errorMessage, autoCorrect)
        }
    }
}
