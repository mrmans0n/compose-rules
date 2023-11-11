// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import io.nlopez.compose.rules.ViewModelInjection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ViewModelInjectionCheckTest {
    private val testConfig = TestConfig(
        "viewModelFactories" to listOf("bananaViewModel", "potatoViewModel"),
    )
    private val rule = ViewModelInjectionCheck(testConfig)

    @ParameterizedTest
    @ValueSource(strings = ["viewModel", "weaverViewModel", "hiltViewModel", "bananaViewModel", "potatoViewModel"])
    fun `passes when a weaverViewModel is used as a default param`(viewModel: String) {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(
                modifier: Modifier,
                viewModel: MyVM = $viewModel(),
                viewModel2: MyVM = $viewModel(),
            ) { }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["viewModel", "weaverViewModel", "hiltViewModel", "bananaViewModel", "potatoViewModel"])
    fun `overridden functions are ignored`(viewModel: String) {
        @Language("kotlin")
        val code =
            """
            @Composable
            override fun Content() {
                val viewModel = $viewModel<MyVM>()
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["viewModel", "weaverViewModel", "hiltViewModel", "bananaViewModel", "potatoViewModel"])
    fun `errors when a weaverViewModel is used at the beginning of a Composable`(viewModel: String) {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(modifier: Modifier) {
                val viewModel = $viewModel<MyVM>()
            }
            @Composable
            fun MyComposableNoParams() {
                val viewModel: MyVM = $viewModel()
            }
            @Composable
            fun MyComposableTrailingLambda(block: () -> Unit) {
                val viewModel: MyVM = $viewModel()
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasSize(3)
            .hasStartSourceLocations(
                SourceLocation(3, 9),
                SourceLocation(7, 9),
                SourceLocation(11, 9),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ViewModelInjection.errorMessage(viewModel))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["viewModel", "weaverViewModel", "hiltViewModel", "bananaViewModel", "potatoViewModel"])
    fun `errors when a weaverViewModel is used in different branches`(viewModel: String) {
        @Language("kotlin")
        val code =
            """
            @Composable
            fun MyComposable(modifier: Modifier) {
                if (blah) {
                    val viewModel = $viewModel<MyVM>()
                } else {
                    val viewModel: MyOtherVM = $viewModel()
                }
            }
            """.trimIndent()
        val errors = rule.lint(code)
        assertThat(errors).hasSize(2)
            .hasStartSourceLocations(
                SourceLocation(4, 13),
                SourceLocation(6, 13),
            )
        for (error in errors) {
            assertThat(error).hasMessage(ViewModelInjection.errorMessage(viewModel))
        }
    }
}
