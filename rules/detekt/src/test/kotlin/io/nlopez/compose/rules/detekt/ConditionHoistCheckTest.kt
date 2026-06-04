// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConditionHoistCheckTest {

    private val rule = ConditionHoistCheck(Config.empty)

    @Test
    fun `reports layout with a single conditional composable child`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(10, 9)
    }

    @Test
    fun `reports layout with a single expression conditional composable child`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    if (showText) Text("Hello")
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(10, 9)
    }

    @Test
    fun `reports expression body layout with a single conditional composable child`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) = Column {
                if (showText) {
                    Text("Hello")
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(9, 5)
    }

    @Test
    fun `reports property getter layout with a single conditional composable child`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            val Content
                @Composable get() = Column {
                    if (showText()) {
                        Text("Hello")
                    }
                }

            fun showText(): Boolean = true
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(9, 9)
    }

    @Test
    fun `does not report conditional with composable else branch`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    if (showText) {
                        Text("Hello")
                    } else {
                        Text("Fallback")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional with single expression composable else branch`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    if (showText) {
                        Text("Hello")
                    } else Text("Fallback")
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional with composable lambda parameter in else branch`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.BasicTextField
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                BasicTextField { innerTextField ->
                    Column {
                        if (showText) {
                            Text("Hint")
                        } else {
                            innerTextField()
                        }
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports conditional when else branch only declares deferred composable content`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    if (showText) {
                        Text("Hello")
                    } else {
                        val content: @Composable () -> Unit = {
                            Text("Fallback")
                        }
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(10, 9)
    }

    @Test
    fun `does not report conditional that only declares deferred composable content`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    if (showText) {
                        val content: @Composable () -> Unit = {
                            Text("Hello")
                        }
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional in one slot when call has another composable slot`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Icon
            import com.example.compose.fake.ListItem
            import com.example.compose.fake.Text

            @Composable
            fun Content(showIcon: Boolean) {
                ListItem(
                    headlineContent = {
                        Text("Title")
                    },
                    trailingContent = {
                        if (showIcon) {
                            Icon()
                        }
                    },
                )
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional in a single slot call with semantic arguments`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.AnimatedVisibility
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Text

            @Composable
            fun Content(expanded: Boolean, showHint: Boolean) {
                AnimatedVisibility(visible = expanded) {
                    if (showHint) {
                        Text("Hint")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional inside a non layout wrapper`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.AppTheme
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                AppTheme {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports configured custom layout with a single conditional composable child`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("DesignColumn")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.DesignColumn
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                DesignColumn {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(10, 9)
    }

    @Test
    fun `keeps default layouts when custom content emitters are configured`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("DesignColumn")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(10, 9)
    }

    @Test
    fun `does not report conditional with composable siblings in the same layout`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    Text("Header")
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional with non composable siblings in the same layout`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Column {
                    val label = "Hello"
                    if (showText) {
                        Text(label)
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional nested in non-layout content`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            data class Item(val name: String, val visible: Boolean)

            @Composable
            fun Content(items: List<Item>) {
                Column {
                    items.forEach { item ->
                        if (item.visible) {
                            Text(item.name)
                        }
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional that depends on a composable receiver scope`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("BoxWithConstraints")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.BoxWithConstraints
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Text

            @Composable
            fun Content() {
                BoxWithConstraints {
                    if (maxWidth > 600) {
                        Text("Wide")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report simple conditional that depends on a composable receiver scope`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("BoxWithConstraints")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.BoxWithConstraints
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Text

            @Composable
            fun Content() {
                BoxWithConstraints {
                    if (shouldShow) {
                        Text("Visible")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional that calls a composable receiver scope function`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("BoxWithConstraints")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.BoxWithConstraints
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Text

            @Composable
            fun Content() {
                BoxWithConstraints {
                    if (shouldShow()) {
                        Text("Visible")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional that depends on receiver scope extension property`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("BoxWithConstraints")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.BoxWithConstraints
            import com.example.compose.fake.BoxWithConstraintsScope
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Text

            val BoxWithConstraintsScope.isExpanded: Boolean
                get() = maxWidth > 600

            @Composable
            fun Content() {
                BoxWithConstraints {
                    if (isExpanded) {
                        Text("Visible")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional that calls a receiver scope extension function`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("BoxWithConstraints")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.BoxWithConstraints
            import com.example.compose.fake.BoxWithConstraintsScope
            import com.example.compose.fake.Composable
            import com.example.compose.fake.Text

            fun BoxWithConstraintsScope.isExpanded(): Boolean = maxWidth > 600

            @Composable
            fun Content() {
                BoxWithConstraints {
                    if (isExpanded()) {
                        Text("Visible")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports scoped layout conditional that does not depend on receiver scope`() {
        val customRule = ConditionHoistCheck(TestConfig("contentEmitters" to listOf("ScopedColumn")))
        val findings = customRule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.ScopedColumn
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                ScopedColumn {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(10, 9)
    }

    @Test
    fun `does not report conditional in a slot lambda with parameters`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Items
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Items(listOf("Hello")) { item ->
                    if (showText) {
                        Text(item)
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditional in a slot lambda with inferred parameters`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Scaffold
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                Scaffold {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report layout with explicit ignored arguments`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Modifier
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean, modifier: Modifier) {
                Column(modifier = modifier) {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report layout with positional explicit ignored arguments`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Modifier
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean, modifier: Modifier) {
                Column(modifier) {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report conditionals outside composable functions`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            fun Content(showText: Boolean) {
                Column {
                    if (showText) {
                        Text("Hello")
                    }
                }
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
            allowCompilationErrors = true,
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports local composable function conditionals only once`() {
        val findings = rule.lintWithAnalysisApi(
            """
            package com.example

            import com.example.compose.fake.Composable
            import com.example.compose.fake.Column
            import com.example.compose.fake.Text

            @Composable
            fun Content(showText: Boolean) {
                @Composable
                fun LocalContent() {
                    Column {
                        if (showText) {
                            Text("Hello")
                        }
                    }
                }

                LocalContent()
            }
            """.trimIndent(),
            fakeLayoutRuntime(),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings[0])
            .hasMessage(ConditionHoistCheck.ConditionCouldBeHoisted)
            .hasStartSourceLocation(12, 13)
    }

    private fun fakeLayoutRuntime(): String = codeWithFakeCompose(
        """
        object Modifier

        @Composable
        fun Column(
            modifier: Modifier = Modifier,
            content: @Composable () -> Unit,
        ) {
            content()
        }

        interface ColumnScope

        @Composable
        fun ScopedColumn(
            modifier: Modifier = Modifier,
            content: @Composable ColumnScope.() -> Unit,
        ) {
            val scope = object : ColumnScope {
            }
            scope.content()
        }

        @Composable
        fun DesignColumn(
            content: @Composable () -> Unit,
        ) {
            content()
        }

        @Composable
        fun AppTheme(
            content: @Composable () -> Unit,
        ) {
            content()
        }

        @Composable
        fun Text(value: String) {
        }

        @Composable
        fun Icon() {
        }

        @Composable
        fun ListItem(
            headlineContent: @Composable () -> Unit,
            trailingContent: @Composable () -> Unit,
        ) {
            headlineContent()
            trailingContent()
        }

        @Composable
        fun AnimatedVisibility(
            visible: Boolean,
            content: @Composable () -> Unit,
        ) {
            if (visible) {
                content()
            }
        }

        @Composable
        fun <T> Items(
            items: List<T>,
            itemContent: @Composable (T) -> Unit,
        ) {
            items.forEach { itemContent(it) }
        }

        @Composable
        fun BasicTextField(
            decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
        ) {
            decorationBox {}
        }

        interface PaddingValues

        @Composable
        fun Scaffold(
            content: @Composable (PaddingValues) -> Unit,
        ) {
            content(object : PaddingValues {
            })
        }

        interface BoxWithConstraintsScope {
            val maxWidth: Int
            val shouldShow: Boolean
            fun shouldShow(): Boolean
        }

        @Composable
        fun BoxWithConstraints(
            content: @Composable BoxWithConstraintsScope.() -> Unit,
        ) {
            val scope = object : BoxWithConstraintsScope {
                override val maxWidth: Int = 0
                override val shouldShow: Boolean = false
                override fun shouldShow(): Boolean = false
            }
            scope.content()
        }
        """.trimIndent(),
    )
}
