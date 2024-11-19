// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ContentSlotReused
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ContentSlotReusedCheckTest {

    private val ruleAssertThat = assertThatRule { ContentSlotReusedCheck() }

    @Test
    fun `errors when there is a slot being reused`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(text: String, content: @Composable () -> Unit) {
                    if (x) content() else content()
                }
                @Composable
                fun B(text: String, content: @Composable () -> Unit) {
                    when {
                        x -> content()
                        else -> content()
                    }
                }
                @Composable
                fun C(text: String, content: @Composable () -> Unit) {
                    potato?.let { content() } ?: content()
                }
                @Composable
                fun D(text: String, content: Potato) {
                    potato?.let { content() } ?: content()
                }
                @Composable
                fun E(text: String, content: @Composable () -> Unit) {
                    val content1 = remember { movableContentOf { content() } }
                    val content2 = remember { movableContentOf { content() } }
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(treatAsComposableLambda to "Potato")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
                LintViolation(
                    line = 6,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
                LintViolation(
                    line = 13,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
                LintViolation(
                    line = 17,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
                LintViolation(
                    line = 21,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
            )
    }

    @Test
    fun `errors when there is a nullable slot being reused`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(text: String, content: @Composable (() -> Unit)? = null) {
                    if (x) content?.invoke() else content?.invoke()
                }
                @Composable
                fun B(text: String, content: @Composable (() -> Unit)? = null) {
                    when {
                        x -> content?.invoke()
                        else -> content?.invoke()
                    }
                }
                @Composable
                fun C(text: String, content: @Composable (() -> Unit)? = null) {
                    val content1 = remember { movableContentOf { content?.invoke() } }
                    val content2 = remember { movableContentOf { content?.invoke() } }
                }
                @Composable
                fun D(content: Potato? = null) {
                    if (x) content?.invoke() else content?.invoke()
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(treatAsComposableLambda to "Potato", treatAsLambda to "Plum")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 2,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
                LintViolation(
                    line = 6,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
                LintViolation(
                    line = 13,
                    col = 21,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
                LintViolation(
                    line = 18,
                    col = 7,
                    detail = ContentSlotReused.ContentSlotsShouldNotBeReused,
                ),
            )
    }

    @Test
    fun `passes when used in a movableContentOf`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: @Composable () -> Unit, text: String) {
                    val content = remember { movableContentOf { content() } }
                    if (x) content() else content()
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(treatAsComposableLambda to "Potato", treatAsLambda to "Plum")
            .hasNoLintViolations()
    }

    @Test
    fun `passes when content is not composable`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(content: () -> Unit, text: String) {
                    if (x) content() else content()
                }
                fun B(content: Plum, text: String) {
                    if (x) content() else content()
                }
                @Composable
                fun C(content: (() -> Unit)? = null) {
                    if (x) content?.invoke() else content?.invoke()
                }
                @Composable
                fun D(content: Plum? = null) {
                    if (x) content?.invoke() else content?.invoke()
                }
            """.trimIndent()

        ruleAssertThat(code)
            .withEditorConfigOverride(treatAsComposableLambda to "Potato", treatAsLambda to "Plum")
            .hasNoLintViolations()
    }

    @Test
    fun `passes when content does not return Unit`() {
        @Language("kotlin")
        val code =
            """
                @Composable
                fun A(text: String, content: @Composable () -> String) {
                    if (x) content() else content()
                }
                @Composable
                fun B(text: String, content: @Composable (() -> String)?) {
                    if (x) content() else content()
                }

            """.trimIndent()

        ruleAssertThat(code).hasNoLintViolations()
    }
}
