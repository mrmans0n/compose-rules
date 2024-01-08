// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.ktlint

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import io.nlopez.rules.core.ComposeKtConfig
import io.nlopez.rules.core.ComposeKtVisitor
import io.nlopez.rules.core.Emitter
import io.nlopez.rules.core.util.isComposable
import io.nlopez.rules.core.util.startOffsetFromName
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class KtlintRule(
    id: String,
    editorConfigProperties: Set<EditorConfigProperty<*>> = emptySet(),
) : Rule(
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

    private val config: ComposeKtConfig by lazy { KtlintComposeKtConfig(properties, usesEditorConfigProperties) }

    final override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (val psi = node.psi) {
            is KtFile -> visitFile(psi, autoCorrect, emit.toEmitter(), config)
            is KtClass -> visitClass(psi, autoCorrect, emit.toEmitter(), config)
            is KtFunction -> {
                val emitter = emit.toEmitter()
                visitFunction(psi, autoCorrect, emitter, config)
                if (psi.isComposable) {
                    visitComposable(psi, autoCorrect, emitter, config)
                }
            }
        }
    }

    private fun ((Int, String, Boolean) -> Unit).toEmitter() = Emitter { element, errorMessage, canBeAutoCorrected ->
        val offset = if (element is PsiNameIdentifierOwner) {
            element.startOffsetFromName
        } else {
            element.startOffset
        }
        invoke(offset, errorMessage, canBeAutoCorrected)
    }
}
