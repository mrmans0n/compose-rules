package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.findChildrenByClass
import io.nlopez.compose.core.util.isModifierReceiver
import io.nlopez.compose.core.util.isSuppressed
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression

class ModifierThenImplicitModifier : ComposeKtVisitor {

    override fun visitFunction(
        function: KtFunction,
        emitter: Emitter,
        config: ComposeKtConfig
    ) {
        if (!function.isModifierReceiver(config)) return

        // This rule is a non-type resolution version of https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-lint/src/main/java/androidx/compose/ui/lint/SuspiciousModifierThenDetector.kt
        // so we should suppress it if the other rule is suppressed already.
        if (function.isSuppressed("SuspiciousModifierThen")) return

        // As we don't have type resolution, we can't be too fancy here. Let's limit the checks to the simplest cases.
        // Let's find all the calls to then, excluding lambdas (because that would mean a different scope).
        val thenCallExpressions = function.findChildrenByClass<KtCallExpression> { it !is KtLambdaExpression }
            .filter { it.calleeExpression?.text == "then" }
            .filter {
                it.parent is KtDotQualifiedExpression
            }
    }

    companion object {
        val ModifierThenImplicitModifierErrorMessage = """
            Modifier.then with a Modifier factory function that has an implicit receiver should be avoided.

            See https://mrmans0n.github.io/compose-rules/rules/#TODO for more information.
        """.trimIndent()
    }
}
