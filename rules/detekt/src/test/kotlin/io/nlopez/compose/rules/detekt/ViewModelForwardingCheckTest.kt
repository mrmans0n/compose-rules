// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ViewModelForwarding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ViewModelForwardingCheckTest {

    private val testConfig = TestConfig(
        "allowedStateHolderNames" to listOf(".*Component", ".*StateHolder"),
        "allowedForwarding" to listOf(".*Content"),
    )
    private val rule = ViewModelForwardingCheck(testConfig)

    @Test
    fun `allows the forwarding of ViewModels in overridden Composable functions`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            override fun Content() {
                val viewModel = weaverViewModel<MyVM>()
                AnotherComposable(viewModel)
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `allows the forwarding of ViewModels in interface Composable functions`() {
        @Language("kotlin")
        val code =
            """
            interface MyInterface {
                @Composable
                fun Content() {
                    val viewModel = weaverViewModel<MyVM>()
                    AnotherComposable(viewModel)
                }
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `using state hoisting properly shouldn't be flagged`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel = weaverViewModel()) {
                val state by viewModel.watchAsState()
                AnotherComposable(state, onAvatarClicked = { viewModel(AvatarClickedIntent) })
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors when a ViewModel is forwarded to another Composable`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                AnotherComposable(viewModel)
            }
            @Composable
            fun MyComposable2(viewModel: MyViewModel) {
                Row {
                    AnotherComposable(viewModel)
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                AnotherComposable(vm = viewModel)
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(3, 5),
            SourceLocation(8, 9),
            SourceLocation(13, 5),
        )
        for (error in errors) {
            assertThat(error).hasMessage(ViewModelForwarding.AvoidViewModelForwarding)
        }
    }

    @Test
    fun `allows the forwarding of ViewModels that are used as keys`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun Content() {
                val viewModel = weaverViewModel<MyVM>()
                key(viewModel) { }
                val x = remember(viewModel) { "ABC" }
                LaunchedEffect(viewModel) { }
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `errors when a custom state holder is forwarded`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewComponent) {
                AnotherComposable(viewModel)
            }
            @Composable
            fun MyComposable2(viewModel: MyStateHolder) {
                AnotherComposable(viewModel)
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasStartSourceLocations(
            SourceLocation(3, 5),
            SourceLocation(7, 5),
        )
        for (error in errors) {
            assertThat(error).hasMessage(ViewModelForwarding.AvoidViewModelForwarding)
        }
    }

    @Test
    fun `allows forwarding when a ViewModel is in the allowlist`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                AnotherComposableContent(viewModel)
            }
            @Composable
            fun MyComposable2(viewModel: MyViewModel) {
                Row {
                    AnotherComposableContent(viewModel)
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                AnotherComposableContent(vm = viewModel)
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }
}
