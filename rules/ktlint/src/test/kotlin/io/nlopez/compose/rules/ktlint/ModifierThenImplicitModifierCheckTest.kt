package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ModifierThenImplicitModifier
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierThenImplicitModifierCheckTest {

    private val ruleAssertThat = assertThatRule { ModifierThenImplicitModifierCheck() }

    @Test
    fun `errors for X case`() {
        @Language("kotlin")
        val code =
            """
            TODO()
            """.trimIndent()
        ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 2,
                col = 5,
                detail = ModifierThenImplicitModifier.ModifierThenImplicitModifierErrorMessage,
            ),
        )
    }

    @Test
    fun `passes for X case`() {
        @Language("kotlin")
        val code =
            """
            TODO()
            """.trimIndent()
        ruleAssertThat(code).hasNoLintViolations()
    }
}
