// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import org.junit.jupiter.api.Test

class UnnecessaryLaunchedEffectCheckTest {

    private val rule = UnnecessaryLaunchedEffectCheck(Config.empty)

    @Test
    fun `reports LaunchedEffect that only calls non-suspending functions`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                fun update(value: String) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        update("done")
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
        assertThat(findings.single()).hasMessage(UnnecessaryLaunchedEffectCheck.UnnecessaryLaunchedEffect)
    }

    @Test
    fun `allows direct nested and operator suspend calls`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                suspend fun fetch() = Unit
                operator suspend fun Int.get(index: Int): Int = index

                class Action {
                    suspend operator fun invoke() = Unit
                }

                data class Number(val value: Int) {
                    suspend operator fun plus(other: Number) = Number(value + other.value)
                }

                @Composable
                fun Example() {
                    LaunchedEffect("direct") {
                        fetch()
                    }
                    LaunchedEffect("nested") {
                        repeat(2) { fetch() }
                    }
                    LaunchedEffect("array") {
                        1[0]
                    }
                    LaunchedEffect("invoke") {
                        Action()()
                    }
                    LaunchedEffect("binary") {
                        Number(1) + Number(2)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows a chained terminal suspend call`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                interface Flow<T> {
                    suspend fun collect(action: (T) -> Unit)
                }

                fun <T> snapshotFlow(block: () -> T): Flow<T> = TODO()
                fun <T, R> Flow<T>.map(transform: (T) -> R): Flow<R> = TODO()

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        snapshotFlow { "state" }
                            .map { it.length }
                            .collect { println(it) }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports a flow chain without a terminal suspend call`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                interface Flow<T>

                fun <T> snapshotFlow(block: () -> T): Flow<T> = TODO()
                fun <T, R> Flow<T>.map(transform: (T) -> R): Flow<R> = TODO()
                fun update(value: Any?) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        val flow = snapshotFlow { "state" }.map { it.length }
                        update(flow)
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports assignments with short-circuit expressions`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                var lastValue = 0

                @Composable
                fun Example(enabled: Boolean) {
                    LaunchedEffect(enabled) {
                        lastValue = if (enabled && lastValue > 0) lastValue else 0
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports referential equality`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                @Composable
                fun Example(first: Any, second: Any) {
                    LaunchedEffect(Unit) {
                        first === second
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports deferred suspend lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                fun register(callback: suspend () -> Unit) = Unit
                suspend fun fetch() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register { fetch() }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows deferred lambdas that capture the LaunchedEffect CoroutineScope`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun register(callback: () -> Unit) = Unit
                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register { consume(this) }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows deferred lambdas that call LaunchedEffect CoroutineScope extensions`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.launch(block: suspend () -> Unit) = Unit
                fun register(callback: () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register {
                            launch { update() }
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports non-trailing effect lambda arguments`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                fun update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit, { update() })
                    LaunchedEffect(key1 = Unit, block = { update() })
                    LaunchedEffect(Unit, block = ({ update() }))
                }
                """,
            ),
        )

        assertThat(findings).hasSize(3)
    }

    @Test
    fun `allows calls on LaunchedEffect CoroutineScope`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.launch(block: suspend () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        launch { update() }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows direct LaunchedEffect CoroutineScope receiver references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        consume(this)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports external CoroutineScope references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        consume(scope)
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports labeled outer CoroutineScope receiver references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun CoroutineScope.Example() {
                    LaunchedEffect(Unit) {
                        consume(this@Example)
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows labeled LaunchedEffect CoroutineScope receiver references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit, block = effect@ {
                        consume(this@effect)
                    })
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows implicit LaunchedEffect label CoroutineScope receiver references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        consume(this@LaunchedEffect)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows aliased LaunchedEffect label CoroutineScope receiver references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import com.example.compose.fake.LaunchedEffect as LE
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LE(Unit) {
                        consume(this@LE)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports calls on external CoroutineScope receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.update() = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        scope.update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports property reads on external CoroutineScope receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                val CoroutineScope.ready: Boolean get() = true
                fun consume(value: Boolean) = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        consume(scope.ready)
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports calls on nested external CoroutineScope implicit receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.update() = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        with(scope) {
                            update()
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports this references in nested external CoroutineScope implicit receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        with(scope) {
                            consume(this)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports calls on run external CoroutineScope implicit receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.update() = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        scope.run {
                            update()
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows LaunchedEffect CoroutineScope calls inside nested non-scope receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.launch(block: suspend () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        with("value") {
                            launch { update() }
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows implicit effect scope calls inside user with lambdas without receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                inline fun with(scope: CoroutineScope, block: () -> Unit) = block()
                fun CoroutineScope.launch(block: suspend () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        with(scope) {
                            launch { update() }
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows LaunchedEffect coroutineContext references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineContext

                fun consume(context: CoroutineContext) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        consume(coroutineContext)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows LaunchedEffect CoroutineScope extension property references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(active: Boolean) = Unit
                val CoroutineScope.isActive: Boolean get() = true

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        consume(isActive)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows calls with a LaunchedEffect CoroutineScope context receiver`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                context(CoroutineScope)
                fun update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows property reads with a LaunchedEffect CoroutineScope context receiver`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                context(CoroutineScope)
                val needsScope: Boolean get() = true

                fun consume(value: Boolean) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        consume(needsScope)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows local functions that capture the LaunchedEffect CoroutineScope`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        fun helper() {
                            consume(this)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows local functions that call LaunchedEffect CoroutineScope extensions`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.launch(block: suspend () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        fun helper() {
                            launch { update() }
                        }
                        helper()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows implicit suspend convention calls`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                class Result {
                    suspend operator fun component1(): String = "value"
                }

                class Items {
                    suspend operator fun iterator(): Iterator<Int> = listOf(1).iterator()
                }

                fun update(value: Any?) = Unit

                @Composable
                fun Example(result: Result, items: Items) {
                    LaunchedEffect(Unit) {
                        val (value) = result
                        update(value)
                    }
                    LaunchedEffect(Unit) {
                        for (item in items) {
                            update(item)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows delegated properties with suspend convention calls`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlin.reflect.KProperty

                class Delegate {
                    suspend operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "value"
                }

                fun consume(value: String) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        val result by Delegate()
                        consume(result)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows callable references bound to LaunchedEffect CoroutineScope extensions`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        val callback: () -> Unit = ::update
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report unresolved calls`() {
        val findings = rule.lintWithAnalysisApi(
            content = codeWithFakeCompose(
                """
                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        unresolvedCall()
                    }
                }
                """,
            ),
            allowCompilationErrors = true,
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report a different function named LaunchedEffect`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                object Other {
                    fun LaunchedEffect(key: Any?, block: suspend () -> Unit) = Unit
                }

                fun update() = Unit

                @Composable
                fun Example() {
                    Other.LaunchedEffect(Unit) {
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows calls on configured receiver types`() {
        val configuredRule = UnnecessaryLaunchedEffectCheck(
            TestConfig(
                "allowedCallReceiverTypes" to listOf("com.example.compose.fake.FocusRequester"),
            ),
        )
        val findings = configuredRule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                class FocusRequester {
                    fun requestFocus() = Unit
                }

                fun FocusRequester.requestFocusSafely() = Unit

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocusSafely()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver calls through local functions`() {
        val configuredRule = UnnecessaryLaunchedEffectCheck(
            TestConfig(
                "allowedCallReceiverTypes" to listOf("com.example.compose.fake.FocusRequester"),
            ),
        )
        val findings = configuredRule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                class FocusRequester {
                    fun requestFocus() = Unit
                }

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        helper()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows calls with configured names`() {
        val configuredRule = UnnecessaryLaunchedEffectCheck(
            TestConfig(
                "allowedCallNames" to listOf("com.example.compose.fake.logScreen"),
            ),
        )
        val findings = configuredRule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                fun logScreen() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        logScreen()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `ignores suspend calls inside unused local functions`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                fun update() = Unit
                suspend fun fetch() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        suspend fun unused() {
                            fetch()
                        }
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }
}
