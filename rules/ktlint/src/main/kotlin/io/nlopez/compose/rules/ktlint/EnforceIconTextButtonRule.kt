package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Custom ktlint rule to enforce the use of IconTextButton instead of Button to maintain consistency across the UI.
 *
 * Maintainer: My stc Team
 * Repository URL: https://github.com/your-repository-url
 * Issue Tracker: https://github.com/your-repository-url/issues
 *
 * ## Non-Compliant Example:
 * ```
 * @Composable
 * fun Example() {
 *     Button(onClick = { /* Do something */ }) {
 *         Text("Click Me")
 *     }
 * }
 * ```
 *
 * ## Compliant Example:
 * ```
 * @Composable
 * fun Example() {
 *     IconTextButton(onClick = { /* Do something */ }) {
 *         Text("Click Me")
 *     }
 * }
 * ```
 */
class EnforceIconTextButtonRule :
    Rule(
        ruleId = RuleId("enforce-icon-text-button"),
        about =
            About(
                maintainer = "My stc Team",
                repositoryUrl = "https://github.com/your-repository-url",
                issueTrackerUrl = "https://github.com/your-repository-url/issues",
            ),
    ) {
    override fun beforeFirstNode(editorConfig: EditorConfig) {
        // Reset state before processing the file.
    }

    @Deprecated(
        "Marked for removal in Ktlint 2.0. Please implement interface RuleAutocorrectApproveHandler.",
    )
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val psi = node.psi
        when (node.elementType) {
            ElementType.CALL_EXPRESSION -> {
                val callExpression = psi as? KtCallExpression ?: return
                visitCallExpression(callExpression, autoCorrect, emit)
            }
        }
    }

    private fun visitCallExpression(
        callExpression: KtCallExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val calleeExpression = callExpression.calleeExpression?.text ?: return

        // Check if the call expression is a Button composable.
        if (calleeExpression == "Button") {
            emit(
                callExpression.startOffset,
                "Use IconTextButton instead of Button to maintain consistency in the UI.",
                autoCorrect,
            )

            // Optional auto-correct logic to replace Button with IconTextButton
            if (autoCorrect) {
                replaceButtonWithIconTextButton(callExpression)
            }
        }
    }

    private fun replaceButtonWithIconTextButton(callExpression: KtCallExpression) {
        val factory = KtPsiFactory(callExpression.project)
        val newCallee = factory.createExpression("IconTextButton")
        callExpression.calleeExpression?.replace(newCallee)
    }
}
