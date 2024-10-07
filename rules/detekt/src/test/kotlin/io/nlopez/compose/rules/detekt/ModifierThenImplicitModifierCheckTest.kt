package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ModifierThenImplicitModifier
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ModifierThenImplicitModifierCheckTest {

    private val rule = ModifierThenImplicitModifierCheck(Config.empty)

    @Test
    fun `errors for X case`() {
        @Language("kotlin")
        val code =
            """
            TODO()
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(2, 5),
        )
        for (error in errors) {
            assertThat(error)
                .hasMessage(ModifierThenImplicitModifier.ModifierThenImplicitModifierErrorMessage)
        }
    }

    @Test
    fun `passes for X case`() {
        @Language("kotlin")
        val code =
            """
            TODO()
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
