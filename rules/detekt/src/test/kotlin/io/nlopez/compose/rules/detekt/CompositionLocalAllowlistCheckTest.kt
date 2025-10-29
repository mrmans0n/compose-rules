// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.SourceLocation
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.nlopez.compose.rules.CompositionLocalAllowlist
import io.nlopez.compose.rules.detekt.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class CompositionLocalAllowlistCheckTest {

    private val testConfig = TestConfig(
        "allowedCompositionLocals" to listOf("LocalBanana", "LocalPotato"),
    )
    private val rule = CompositionLocalAllowlistCheck(testConfig)

    @Test
    fun `error when a CompositionLocal is defined`() {
        @Language("kotlin")
        val code =
            """
                private val LocalApple = staticCompositionLocalOf<String> { "Apple" }
                internal val LocalPlum: String = staticCompositionLocalOf { "Plum" }
                val LocalPrune = compositionLocalOf { "Prune" }
                private val LocalKiwi: String = compositionLocalOf { "Kiwi" }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors)
            .hasStartSourceLocations(
                SourceLocation(1, 13),
                SourceLocation(2, 14),
                SourceLocation(3, 5),
                SourceLocation(4, 13),
            )
        for (error in errors) {
            assertThat(error).hasMessage(CompositionLocalAllowlist.CompositionLocalNotInAllowlist)
        }
    }

    @Test
    fun `passes when a CompositionLocal is defined but it's in the allowlist`() {
        @Language("kotlin")
        val code =
            """
                val LocalBanana = staticCompositionLocalOf<String> { "Banana" }
                val LocalPotato = compositionLocalOf { "Potato" }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
