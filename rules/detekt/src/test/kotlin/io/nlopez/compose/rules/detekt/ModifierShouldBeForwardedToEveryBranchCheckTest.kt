// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import io.nlopez.compose.rules.ModifierShouldBeForwardedToEveryBranch
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ModifierShouldBeForwardedToEveryBranchCheckTest {
    private val rule = ModifierShouldBeForwardedToEveryBranchCheck(Config.empty)

    @ParameterizedTest
    @ValueSource(
        strings = [
            "if (state == 0) LoadedContent(modifier) else LoadingContent(androidx.compose.ui.Modifier)",
            "if (state == 0) LoadedContent(modifier) else LoadingContent(Modifier.Companion)",
            "if (state == 0) LoadedContent(modifier) else LoadingContent(androidx.compose.ui.Modifier.padding())",
            "when (state) { 0 -> LoadedContent(modifier); else -> LoadingContent() }",
            "if (state == 0) LoadedContent(modifier.then(Modifier)) else LoadingContent()",
            "if (state == 0) LoadedContent(Modifier.then(modifier)) else LoadingContent()",
            "if (state == 0) LoadedContent(modifier.padding()) else LoadingContent(Modifier.padding())",
            "if (state == 0) LoadedContent(label = \"ready\", modifier) else LoadingContent()",
            "if (state == 0) LoadedContent(modifier = modifier) else LoadingContent(Modifier)",
            "if (state == 0) LoadedContent(modifier) else if (state == 1) LoadingContent() else LoadedContent(modifier)",
            "if (state == 0) Components.Loaded(modifier) else Components.Loading()",
        ],
    )
    fun `reports missing forwarding with resolved calls in another file`(body: String) {
        val findings = rule.lintWithAnalysisApi(content(body), modifierRuntime, branchRuntime)
        assertThat(findings).hasSize(1)
        assertThat(findings.single()).hasMessage(ModifierShouldBeForwardedToEveryBranch.MissingBranchModifier)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "if (state == 0) LoadedContent(modifier) else LoadingContent(modifier)",
            "if (state == 0) LoadedContent(modifier.padding()) else LoadingContent(Modifier.then(modifier))",
            "if (state == 0) LoadedContent(modifier) else LoadingContent(factory(modifier))",
            "if (state == 0) LoadedContent() else LoadingContent()",
            "if (state == 0) LoadedContent(modifier) else EmptyContent()",
            "if (state == 0) LoadedContent(modifier) else ValueContent()",
            "if (state == 0) LoadedContent(modifier) else ReadOnlyContent()",
            "if (state == 0) LoadedContent(modifier) else PlainContent()",
            "when (val modifier = Modifier) { Modifier -> LoadingContent(); else -> LoadedContent(modifier) }",
            "if (state == 0) LoadingContent(otherModifier) else LoadedContent(modifier)",
            "if (state == 0) LoadingContent(if (enabled) modifier else Modifier) else LoadedContent(modifier)",
            "Container(modifier) { if (state == 0) LoadingContent() else LoadedContent() }",
            "EmptyContent()\nif (state == 0) LoadingContent() else LoadedContent(modifier)",
        ],
    )
    fun `ignores branches without definite missing forwarding`(body: String) {
        assertThat(rule.lintWithAnalysisApi(content(body), modifierRuntime, branchRuntime)).isEmpty()
    }

    @Test
    fun `reports the omitted call rather than its forwarding sibling`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package sample
            import com.example.compose.fake.Composable
            import com.example.compose.fake.LoadedContent
            import com.example.compose.fake.LoadingContent
            import androidx.compose.ui.Modifier

            @Composable
            fun Content(loading: Boolean, modifier: Modifier) {
                if (loading) {
                    LoadedContent(modifier)
                } else {
                    LoadingContent()
                }
            }
            """.trimIndent(),
            modifierRuntime,
            branchRuntime,
        )
        assertThat(findings).hasStartSourceLocations(SourceLocation(12, 9))
    }

    @Test
    fun `resolves modifier type aliases`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import androidx.compose.ui.Modifier
                typealias LayoutModifier = Modifier
                @Composable
                fun Content(state: Int, modifier: LayoutModifier = Modifier) {
                    if (state == 0) LoadedContent(modifier) else LoadingContent()
                }
                """,
            ),
            modifierRuntime,
            branchRuntime,
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not mistake a shadowing slot for a same named declaration`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import androidx.compose.ui.Modifier
                @Composable
                fun Content(state: Int, modifier: Modifier, LoadingContent: @Composable () -> Unit) {
                    if (state == 0) LoadedContent(modifier) else LoadingContent()
                }
                """,
            ),
            modifierRuntime,
            branchRuntime,
        )
        assertThat(findings).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "if (state == 0) slot(modifier) else LoadingContent()",
            "if (state == 0) LoadedContent(modifier) else slot(Modifier)",
        ],
    )
    fun `reports missing forwarding for modifier accepting slots`(body: String) {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import androidx.compose.ui.Modifier
                @Composable
                fun Content(state: Int, modifier: Modifier, slot: @Composable (Modifier) -> Unit) {
                    $body
                }
                """,
            ),
            modifierRuntime,
            branchRuntime,
        )
        assertThat(findings).hasSize(1)
    }

    private fun content(body: String): String = codeWithFakeCompose(
        """
        import androidx.compose.ui.Modifier
        @Composable
        fun Content(state: Int, modifier: Modifier = Modifier, otherModifier: Modifier = Modifier, enabled: Boolean = true) {
            $body
        }
        """,
    )

    private val modifierRuntime = """
        package androidx.compose.ui
        interface Modifier {
            fun then(other: Modifier): Modifier = this
            companion object : Modifier
        }
    """.trimIndent()

    private val branchRuntime = codeWithFakeCompose(
        """
        import androidx.compose.ui.Modifier
        @Composable fun LoadedContent(modifier: Modifier = Modifier) {}
        @Composable fun LoadedContent(label: String, modifier: Modifier = Modifier) {}
        @Composable fun LoadingContent(modifier: Modifier = Modifier) {}
        fun Modifier.padding(): Modifier = this
        fun factory(modifier: Modifier): Modifier = Modifier
        @Composable fun EmptyContent() {}
        @Composable fun ValueContent(modifier: Modifier = Modifier): Int = 1
        @Composable @ReadOnlyComposable fun ReadOnlyContent(modifier: Modifier = Modifier) {}
        fun PlainContent(modifier: Modifier = Modifier) {}
        @Composable fun Container(modifier: Modifier, content: @Composable () -> Unit) { content() }
        object Components {
            @Composable fun Loaded(modifier: Modifier = Modifier) {}
            @Composable fun Loading(modifier: Modifier = Modifier) {}
        }
        """,
    )
}
