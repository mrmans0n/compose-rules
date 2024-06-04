// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Decision
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.startOffsetFromName
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class KtlintRule(id: String, editorConfigProperties: Set<EditorConfigProperty<*>> = emptySet()) :
    Rule(
        ruleId = RuleId(id),
        about = About(
            maintainer = "Compose Rules",
            repositoryUrl = "https://github.com/mrmans0n/compose-rules",
            issueTrackerUrl = "https://github.com/mrmans0n/compose-rules/issues",
        ),
        usesEditorConfigProperties = editorConfigProperties,
    ),
    ComposeKtVisitor,
    RuleAutocorrectApproveHandler {

    private lateinit var properties: EditorConfig

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        properties = editorConfig
    }

    private val config: ComposeKtConfig by lazy { KtlintComposeKtConfig(properties, usesEditorConfigProperties) }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (val psi = node.psi) {
            is KtFile -> visitFile(psi, emit.toEmitter(), config)
            is KtClass -> visitClass(psi, emit.toEmitter(), config)
            is KtFunction -> {
                val emitter = emit.toEmitter()
                visitFunction(psi, emitter, config)
                if (psi.isComposable) {
                    visitComposable(psi, emitter, config)
                }
            }
        }
    }

    private fun ((Int, String, Boolean) -> AutocorrectDecision).toEmitter() =
        Emitter { element, errorMessage, canBeAutoCorrected ->
            val offset = if (element is PsiNameIdentifierOwner) {
                element.startOffsetFromName
            } else {
                element.startOffset
            }
            when (invoke(offset, errorMessage, canBeAutoCorrected)) {
                AutocorrectDecision.ALLOW_AUTOCORRECT -> Decision.Fix
                AutocorrectDecision.NO_AUTOCORRECT -> Decision.Ignore
            }
        }
}
