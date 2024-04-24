// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import io.nlopez.compose.rules.ViewModelForwarding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ViewModelForwardingCheckTest {

    private val forwardingRuleAssertThat = assertThatRule { ViewModelForwardingCheck() }

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
        forwardingRuleAssertThat(code).hasNoLintViolations()
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
        forwardingRuleAssertThat(code).hasNoLintViolations()
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
        forwardingRuleAssertThat(code).hasNoLintViolations()
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
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(allowedStateHolderNames to ".*Component,.*ViewModel")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 3,
                    col = 5,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 8,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 13,
                    col = 5,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
            )
    }

    @Test
    fun `errors when a ViewModel is forwarded to another Composable within a with scope`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                with(viewModel) {
                    AnotherComposable(this)
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                with(viewModel) {
                    AnotherComposable(vm = this)
                }
            }
            """.trimIndent()
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(allowedStateHolderNames to ".*Component,.*ViewModel")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 4,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 10,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
            )
    }

    @Test
    fun `errors when a ViewModel is forwarded to another Composable within an apply scope`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                viewModel.apply {
                    AnotherComposable(this)
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                viewModel.apply {
                    AnotherComposable(vm = this)
                }
            }
            """.trimIndent()
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(allowedStateHolderNames to ".*Component,.*ViewModel")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 4,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 10,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
            )
    }

    @Test
    fun `errors when a ViewModel is forwarded to another Composable within a run scope`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                viewModel.run {
                    AnotherComposable(this)
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                viewModel.run {
                    AnotherComposable(vm = this)
                }
            }
            """.trimIndent()
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(allowedStateHolderNames to ".*Component,.*ViewModel")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 4,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 10,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
            )
    }

    @Test
    fun `errors when a ViewModel is forwarded to another Composable within a let scope`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                viewModel.let {
                    AnotherComposable(it)
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                viewModel.let {
                    AnotherComposable(vm = it)
                }
            }
            """.trimIndent()
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(allowedStateHolderNames to ".*Component,.*ViewModel")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 4,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 10,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
            )
    }

    @Test
    fun `errors when a ViewModel is forwarded to another Composable within an also scope`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                viewModel.also {
                    AnotherComposable(it)
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                viewModel.also {
                    AnotherComposable(vm = it)
                }
            }
            """.trimIndent()
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(allowedStateHolderNames to ".*Component,.*ViewModel")
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 4,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 10,
                    col = 9,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
            )
    }

    @Test
    fun `allows non first level scope functions`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: MyViewModel) {
                viewModel.also {
                    Row {
                        AnotherComposable(it)
                    }
                }
            }
            @Composable
            fun MyComposable2(viewModel: MyViewModel) {
                viewModel.let {
                    SomeComposable {
                        AnotherComposable(it)
                    }
                }
            }
            @Composable
            fun MyComposable3(viewModel: MyViewModel) {
                viewModel.run {
                    Row {
                        AnotherComposable(this)
                    }
                }
            }
            @Composable
            fun MyComposable4(viewModel: MyViewModel) {
                viewModel.apply {
                    Row {
                        AnotherComposable(this)
                    }
                }
            }
            @Composable
            fun MyComposable5(viewModel: MyViewModel) {
                with(viewModel) {
                    SomeComposable {
                        AnotherComposable(this)
                    }
                }
            }
            """.trimIndent()
        forwardingRuleAssertThat(code).hasNoLintViolations()
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
        forwardingRuleAssertThat(code).hasNoLintViolations()
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
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(
                allowedStateHolderNames to ".*Component,.*StateHolder",
            )
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(
                    line = 3,
                    col = 5,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
                LintViolation(
                    line = 7,
                    col = 5,
                    detail = ViewModelForwarding.AvoidViewModelForwarding,
                ),
            )
    }

    @Test
    fun `allows forwarding when a composable is in the allowlist`() {
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
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(
                allowedForwarding to ".*Content",
            )
            .hasNoLintViolations()
    }

    @Test
    fun `allows forwarding when a ViewModel is in the allowlist`() {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(viewModel: PotatoViewModel) {
                AnotherComposable(viewModel)
            }
            @Composable
            fun MyComposable2(viewModel: PotatoViewModel) {
                Row {
                    AnotherComposable(viewModel)
                }
            }
            @Composable
            fun MyComposable3(viewModel: PotatoViewModel) {
                AnotherComposable(vm = viewModel)
            }
            @Composable
            fun MyComposable4(viewModel: PotatoViewModel) {
                with(viewModel) { AnotherComposable(vm = this) }
            }
            """.trimIndent()
        forwardingRuleAssertThat(code)
            .withEditorConfigOverride(
                allowedForwardingOfTypes to "PotatoViewModel",
            )
            .hasNoLintViolations()
    }
}
