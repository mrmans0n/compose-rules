// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.SourceLocation
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class StaleRememberUpdatedStateInRememberCheckTest {

    private val rule = StaleRememberUpdatedStateInRememberCheck(Config.empty)

    @Test
    fun `reports rememberUpdatedState delegated value read directly inside remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports rememberUpdatedState state value read directly inside remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss = rememberUpdatedState(onDismiss)
                val holder = remember {
                    Holder(latestOnDismiss.value)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `does not report rememberUpdatedState state object passed inside remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss = rememberUpdatedState(onDismiss)
                val holder = remember {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: State<() -> Unit>)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report rememberUpdatedState state value when remember is keyed by source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss = rememberUpdatedState(onDismiss)
                val holder = remember(onDismiss) {
                    Holder(latestOnDismiss.value)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report rememberUpdatedState state value when remember is keyed by state value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss = rememberUpdatedState(onDismiss)
                val holder = remember(latestOnDismiss.value) {
                    Holder(latestOnDismiss.value)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports fully qualified rememberUpdatedState delegated value read directly inside remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by com.example.compose.fake.rememberUpdatedState(onDismiss)
                val holder = remember {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports rememberUpdatedState delegated value read inside named remember calculation`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit, other: Any) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember(calculation = {
                    Holder(latestOnDismiss)
                }, keys = arrayOf(other))
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports rememberUpdatedState delegated value read directly inside rememberSaveable and retain`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit, onConfirm: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val latestOnConfirm by rememberUpdatedState(onConfirm)
                val dismissHolder = rememberSaveable {
                    Holder(latestOnDismiss)
                }
                val confirmHolder = retain {
                    Holder(latestOnConfirm)
                }
            }

            class Holder(val callback: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(2)
            .hasStartSourceLocations(
                SourceLocation(9, 24),
                SourceLocation(12, 24),
            )
        for (finding in findings) {
            assertThat(finding)
                .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
        }
    }

    @Test
    fun `reports rememberUpdatedState delegated value read when rememberSaveable key is not an input`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(key: String, onDismiss: () -> Unit) {
                val latestKey by rememberUpdatedState(key)
                val holder = rememberSaveable(key = key, calculation = {
                    Holder(latestKey.length)
                })
            }

            class Holder(val length: Int)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports rememberUpdatedState delegated value read inside named rememberSaveable init`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(key: String, onDismiss: () -> Unit) {
                val latestKey by rememberUpdatedState(key)
                val holder = rememberSaveable(init = {
                    Holder(latestKey.length)
                }, key = key)
            }

            class Holder(val length: Int)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports read when nested out-of-scope rememberUpdatedState property has the same name`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit, other: () -> Unit, condition: Boolean) {
                val latest by rememberUpdatedState(onDismiss)
                if (condition) {
                    val latest by rememberUpdatedState(other)
                }
                val holder = remember {
                    Holder(latest)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(11, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `does not report when remember is keyed by the rememberUpdatedState source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember(onDismiss) {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when remember is keyed by named array containing rememberUpdatedState source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember(keys = arrayOf(onDismiss), calculation = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when remember is keyed by array variable containing source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val keys = arrayOf(onDismiss)
                val holder = remember(keys = keys, calculation = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when remember key array variable is mutable`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                var keys = arrayOf(onDismiss)
                keys = arrayOf(Unit)
                val holder = remember(keys = keys, calculation = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(10, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports when remember key array variable contents are mutated`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val keys = arrayOf(onDismiss)
                keys[0] = {}
                val holder = remember(keys = keys, calculation = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(10, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports when remember key array variable contents are mutated from eager run lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val keys = arrayOf(onDismiss)
                run {
                    keys[0] = {}
                }
                val holder = remember(keys = keys, calculation = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(12, 24))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `does not report when remember key array variable is mutated only from a deferred lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val keys = arrayOf(onDismiss)
                val mutateLater = { keys[0] = {} }
                val holder = remember(keys = keys, calculation = {
                    Holder(latestOnDismiss)
                })
                mutateLater()
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when shadowed key array variable contents are mutated before remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val keys = arrayOf(onDismiss)
                run {
                    val keys = arrayOf(Unit)
                    keys[0] = Unit
                }
                val holder = remember(keys = keys, calculation = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when rememberSaveable is keyed by inputs containing rememberUpdatedState source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = rememberSaveable(inputs = arrayOf(onDismiss), init = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when rememberSaveable is keyed by inputs variable containing source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val inputs = arrayOf(onDismiss)
                val holder = rememberSaveable(inputs = inputs, init = {
                    Holder(latestOnDismiss)
                })
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when retain is keyed by spread array containing rememberUpdatedState source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val keys = arrayOf(onDismiss)
                val holder = retain(*keys) {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports rememberUpdatedState delegated value read inside eager run lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    run {
                        Holder(latestOnDismiss)
                    }
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(9, 28))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports rememberUpdatedState delegated value read inside eager let lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    onDismiss.let {
                        Holder(latestOnDismiss)
                    }
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(9, 28))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `reports rememberUpdatedState delegated value read inside eager takeIf and takeUnless lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: (() -> Unit)?) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    val enabled = onDismiss.takeIf { latestOnDismiss != null }
                    val disabled = onDismiss.takeUnless { latestOnDismiss != null }
                    Holder(enabled, disabled)
                }
            }

            class Holder(val enabled: (() -> Unit)?, val disabled: (() -> Unit)?)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(2)
            .hasStartSourceLocations(
                SourceLocation(8, 50),
                SourceLocation(9, 55),
            )
        for (finding in findings) {
            assertThat(finding)
                .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
        }
    }

    @Test
    fun `reports rememberUpdatedState delegated value copied to local property inside remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    val captured = latestOnDismiss
                    Holder(captured)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(8, 32))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `does not report rememberUpdatedState read inside custom deferred run lambda`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Scheduler {
                fun run(block: () -> Unit) {}
            }

            @Composable
            fun Example(onDismiss: () -> Unit, scheduler: Scheduler) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    scheduler.run {
                        latestOnDismiss()
                    }
                    Holder(onDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when remember key shadows rememberUpdatedState source name`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    val onDismiss = {}
                    remember(onDismiss) {
                        Holder(latestOnDismiss)
                    }
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(10, 28))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `does not report when remember is keyed by qualified rememberUpdatedState source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Model(val onDismiss: () -> Unit)

            @Composable
            fun Example(model: Model) {
                val latestOnDismiss by rememberUpdatedState(model.onDismiss)
                val holder = remember(model.onDismiss) {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when remember is keyed by nested qualified rememberUpdatedState source value`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Model(val callbacks: Callbacks)
            class Callbacks(val onDismiss: () -> Unit)

            @Composable
            fun Example(model: Model) {
                val latestOnDismiss by rememberUpdatedState(model.callbacks.onDismiss)
                val holder = remember(model.callbacks.onDismiss) {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when qualified remember key receiver shadows source receiver`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            class Model(val onDismiss: () -> Unit)

            @Composable
            fun Example(model: Model, other: Model) {
                val latestOnDismiss by rememberUpdatedState(model.onDismiss)
                val holder = remember {
                    val model = other
                    remember(model.onDismiss) {
                        Holder(latestOnDismiss)
                    }
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single())
            .hasStartSourceLocation(SourceLocation(12, 28))
            .hasMessage(StaleRememberUpdatedStateInRememberCheck.StaleRememberUpdatedStateInRemember)
    }

    @Test
    fun `does not report deferred reads inside nested lambdas`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    Holder(onDismiss = { latestOnDismiss() })
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report shadowed rememberUpdatedState name inside remember`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    val latestOnDismiss = onDismiss
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report deferred reads inside nested functions`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    fun invokeLater() {
                        latestOnDismiss()
                    }
                    Holder(::invokeLater)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report deferred reads inside property getters`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            interface Callback {
                val isEnabled: Boolean
            }

            @Composable
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val callback = remember {
                    object : Callback {
                        override val isEnabled: Boolean
                            get() = latestOnDismiss.hashCode() > 0
                    }
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report rememberUpdatedState property declaration itself`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            @Composable
            fun Example(onDismiss: () -> Unit) {
                val holder = remember {
                    val latestOnDismiss by rememberUpdatedState(onDismiss)
                }
            }
            """,
        )

        val findings = rule.lintWithAnalysisApi(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report outside composable scope`() {
        @Language("kotlin")
        val code = codeWithFakeCompose(
            """
            fun Example(onDismiss: () -> Unit) {
                val latestOnDismiss by rememberUpdatedState(onDismiss)
                val holder = remember {
                    Holder(latestOnDismiss)
                }
            }

            class Holder(val onDismiss: () -> Unit)
            """,
        )

        val findings = rule.lintWithAnalysisApi(code, allowCompilationErrors = true)

        assertThat(findings).isEmpty()
    }
}
