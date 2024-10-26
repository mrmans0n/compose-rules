package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.rules.KtlintRule
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Decision
import io.nlopez.compose.core.Emitter
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class EnforceIconTextButtonRule22 :
    KtlintRule(
        id = "compose:enforce-icon-text-button",
        editorConfigProperties = emptySet(),
    ), ComposeKtVisitor {
    private lateinit var properties: EditorConfig

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        properties = editorConfig
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected: Boolean,
        ) -> AutocorrectDecision,
    ) {
        val psi = node.psi
        if (psi is KtCallExpression) {
            visitCallExpression(psi, emit.toEmitter())
        }
    }

    private fun visitCallExpression(
        callExpression: KtCallExpression,
        emitter: Emitter,
    ) {
        val calleeExpression = callExpression.calleeExpression?.text ?: return

        // Check if the call expression is a Button composable.
        if (calleeExpression == "Button") {
            emitter.report(
                callExpression,
                "Use IconTextButton instead of Button to maintain consistency in the UI.",
                canBeAutoCorrected = true,
            )
        }
    }

    private fun ((Int, String, Boolean) -> AutocorrectDecision).toEmitter() =
        Emitter { element, errorMessage, canBeAutoCorrected ->
            val offset = element.startOffset
            when (invoke(offset, errorMessage, canBeAutoCorrected)) {
                AutocorrectDecision.ALLOW_AUTOCORRECT -> Decision.Fix
                AutocorrectDecision.NO_AUTOCORRECT -> Decision.Ignore
            }
        }
}
