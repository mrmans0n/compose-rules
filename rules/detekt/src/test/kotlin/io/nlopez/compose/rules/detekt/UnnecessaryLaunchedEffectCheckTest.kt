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
    fun `reports deferred suspend CoroutineScope receiver lambdas with context receiver calls`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun register(callback: suspend CoroutineScope.() -> Unit) = Unit

                context(CoroutineScope)
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
    fun `reports deferred suspend lambdas inside invoked local lambda values`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                fun register(callback: suspend () -> Unit) = Unit
                suspend fun fetch() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        val callback = {
                            register { fetch() }
                        }
                        callback()
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
    fun `allows explicitly qualified calls with LaunchedEffect CoroutineScope context receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                class Foo

                context(CoroutineScope)
                fun Foo.update() = Unit

                @Composable
                fun Example(foo: Foo) {
                    LaunchedEffect(Unit) {
                        foo.update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows explicitly qualified context receiver calls inside local functions and deferred lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                class Foo

                context(CoroutineScope)
                fun Foo.update() = Unit

                fun register(callback: () -> Unit) = Unit

                @Composable
                fun Example(foo: Foo) {
                    LaunchedEffect("local") {
                        fun helper() {
                            foo.update()
                        }
                    }
                    LaunchedEffect("deferred") {
                        register {
                            foo.update()
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
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
    fun `allows explicitly qualified properties with LaunchedEffect CoroutineScope context receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                class Foo

                context(CoroutineScope)
                val Foo.ready: Boolean get() = true

                fun consume(value: Boolean) = Unit

                @Composable
                fun Example(foo: Foo) {
                    LaunchedEffect(Unit) {
                        consume(foo.ready)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
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
    fun `reports this references in nested custom CoroutineScope receiver lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun register(block: CoroutineScope.() -> Unit) = Unit
                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register {
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
    fun `reports calls in nested custom CoroutineScope subtype receiver lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                interface MyScope : CoroutineScope

                fun register(block: MyScope.() -> Unit) = Unit
                fun CoroutineScope.update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register {
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
    fun `allows effect scope receiver calls inside nullable CoroutineScope receiver lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun register(block: (CoroutineScope?).() -> Unit) = Unit
                fun CoroutineScope.update() = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register {
                            update()
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports labeled this references in nested custom CoroutineScope receiver lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun register(block: CoroutineScope.() -> Unit) = Unit
                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register nested@ {
                            consume(this@nested)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports shadowed implicit LaunchedEffect labels in nested custom CoroutineScope receiver lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun register(block: CoroutineScope.() -> Unit) = Unit
                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        register LaunchedEffect@ {
                            consume(this@LaunchedEffect)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports shadowed implicit LaunchedEffect call site labels in nested custom CoroutineScope receiver lambdas`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                object Other {
                    fun LaunchedEffect(block: CoroutineScope.() -> Unit) = Unit
                }

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        Other.LaunchedEffect {
                            consume(this@LaunchedEffect)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports this references in local CoroutineScope receiver functions`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun consume(scope: CoroutineScope) = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        fun CoroutineScope.helper() {
                            consume(this)
                        }
                        scope.helper()
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
    fun `allows implicit effect scope calls inside user run and apply lambdas without receivers`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                fun CoroutineScope.run(block: () -> Unit) = block()
                fun CoroutineScope.apply(block: () -> Unit) = block()
                fun CoroutineScope.launch(block: suspend () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        scope.run {
                            launch { update() }
                        }
                    }
                    LaunchedEffect(Unit) {
                        scope.apply {
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
    fun `allows top-level suspend coroutineContext references`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlin.coroutines.coroutineContext as currentCoroutineContext

                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        currentCoroutineContext
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
    fun `reports explicit CoroutineScope receivers in operator syntax`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                operator fun CoroutineScope.plus(value: String) = Unit
                operator fun CoroutineScope.get(index: Int) = Unit
                operator fun CoroutineScope.unaryPlus() = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect("binary") {
                        scope + "value"
                    }
                    LaunchedEffect("array") {
                        scope[0]
                    }
                    LaunchedEffect("unary") {
                        +scope
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(3)
    }

    @Test
    fun `reports explicit CoroutineScope receivers in loop syntax`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                operator fun CoroutineScope.iterator(): Iterator<String> = listOf("value").iterator()
                fun consume(value: String) = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        for (item in scope) {
                            consume(item)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports explicit CoroutineScope receivers in loop iterator protocol syntax`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                class Items

                operator fun Items.iterator(): CoroutineScope = TODO()
                operator fun CoroutineScope.hasNext(): Boolean = false
                operator fun CoroutineScope.next(): String = "value"
                fun consume(value: String) = Unit

                @Composable
                fun Example(items: Items) {
                    LaunchedEffect(Unit) {
                        for (item in items) {
                            consume(item)
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports explicit CoroutineScope receivers in destructuring syntax`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlinx.coroutines.CoroutineScope

                operator fun CoroutineScope.component1(): String = "value"
                fun consume(value: String) = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        val (item) = scope
                        consume(item)
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports explicit CoroutineScope receivers in delegated property syntax`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                import kotlin.reflect.KProperty
                import kotlinx.coroutines.CoroutineScope

                operator fun CoroutineScope.getValue(thisRef: Any?, property: KProperty<*>): String = "value"
                fun consume(value: String) = Unit

                @Composable
                fun Example(scope: CoroutineScope) {
                    LaunchedEffect(Unit) {
                        val result by scope
                        consume(result)
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
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
    fun `allows callable references bound to LaunchedEffect receiver supertypes`() {
        val findings = rule.lintWithAnalysisApi(
            codeWithFakeCompose(
                """
                @Composable
                fun Example() {
                    LaunchedEffect(Unit) {
                        val callback: () -> String = ::toString
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
    fun `allows configured receiver calls through local function references`() {
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
                    LaunchedEffect("direct") {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        (::helper)()
                    }
                    LaunchedEffect("stored") {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        val callback = ::helper
                        callback()
                    }
                    LaunchedEffect("invoke") {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        val callback = ::helper
                        callback.invoke()
                    }
                    LaunchedEffect("parenthesized") {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        val callback = (::helper)
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports configured receiver calls inside unused local functions`() {
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

                fun update() = Unit

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        fun unused() {
                            focusRequester.requestFocus()
                        }
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver calls inside local function references passed as arguments`() {
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

                fun register(callback: () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        register(::helper)
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver calls inside local functions called from deferred lambdas`() {
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

                fun register(callback: () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        register { helper() }
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver calls inside transitively unused local functions`() {
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

                fun update() = Unit

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        fun unused() {
                            helper()
                        }
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows configured receiver calls through invoked local lambda values`() {
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
                        val callback = {
                            focusRequester.requestFocus()
                        }
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver calls through invoked anonymous function values`() {
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
                        val callback = fun() {
                            focusRequester.requestFocus()
                        }
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver calls through immediately invoked lambda literals`() {
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
                        ({
                            focusRequester.requestFocus()
                        })()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver calls through local lambda values invoked from local functions`() {
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
                        val callback = {
                            focusRequester.requestFocus()
                        }
                        fun helper() {
                            callback()
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
    fun `allows configured receiver calls through local functions invoked from local lambda values`() {
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
                        val callback = {
                            helper()
                        }
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver calls through invoked bound function references`() {
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
                        val callback = focusRequester::requestFocus
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver references passed to inline calls`() {
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

                inline fun immediately(block: () -> Unit) = block()

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        immediately(focusRequester::requestFocus)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver calls inside anonymous functions passed to inline calls`() {
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

                inline fun immediately(block: () -> Unit) = block()

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        immediately(fun() {
                            focusRequester.requestFocus()
                        })
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver references stored and passed to inline calls`() {
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

                inline fun immediately(block: () -> Unit) = block()

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        val callback = focusRequester::requestFocus
                        immediately(callback)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver references invoked through local aliases`() {
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
                        val callback = focusRequester::requestFocus
                        val alias = callback
                        alias()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver references assigned to local variables`() {
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
                        var callback: () -> Unit = {}
                        callback = focusRequester::requestFocus
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver calls inside lambdas assigned to local variables`() {
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
                        var callback: () -> Unit = {}
                        callback = { focusRequester.requestFocus() }
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports configured receiver references assigned after local variable invocation`() {
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
                        var callback: () -> Unit = {}
                        callback()
                        callback = focusRequester::requestFocus
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver references overwritten before local variable invocation`() {
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
                        var callback: () -> Unit = {}
                        callback = focusRequester::requestFocus
                        callback = {}
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows configured receiver references invoked through assigned local aliases`() {
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
                        val callback = focusRequester::requestFocus
                        var alias: () -> Unit = {}
                        alias = callback
                        alias()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows initialized configured receiver references with unreachable overwrites`() {
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
                        var callback: () -> Unit = focusRequester::requestFocus
                        val unused = { callback = {} }
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured receiver references invoked through local functions called after assignment`() {
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
                        var callback: () -> Unit = {}
                        fun invoke() {
                            callback()
                        }
                        callback = focusRequester::requestFocus
                        invoke()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports configured receiver references invoked through local functions called before assignment`() {
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
                        var callback: () -> Unit = {}
                        fun invoke() {
                            callback()
                        }
                        invoke()
                        callback = focusRequester::requestFocus
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver references invoked through overwritten local aliases`() {
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
                        val callback: () -> Unit = focusRequester::requestFocus
                        var alias: () -> Unit = callback
                        alias = {}
                        alias()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports initialized configured receiver references overwritten before local variable invocation`() {
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
                        var callback = focusRequester::requestFocus
                        callback = {}
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows configured local helper references assigned to local variables`() {
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
                        var callback: () -> Unit = {}
                        callback = ::helper
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports configured local helper references assigned after local variable invocation`() {
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
                        var callback: () -> Unit = {}
                        callback()
                        callback = ::helper
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows configured local helper references invoked before later reassignment`() {
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
                        fun first() {
                            focusRequester.requestFocus()
                        }
                        fun second() = Unit
                        var callback = ::first
                        callback()
                        callback = ::second
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports initialized configured receiver lambdas overwritten before local variable invocation`() {
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
                        var callback = { focusRequester.requestFocus() }
                        callback = {}
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured local helper calls inside overwritten initializer lambdas`() {
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
                        var callback = { helper() }
                        callback = {}
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver callback aliases created before assignment`() {
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
                        var callback: () -> Unit = {}
                        val alias = callback
                        callback = focusRequester::requestFocus
                        alias()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows configured local helpers inside lambdas assigned to local variables`() {
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
                        var callback: () -> Unit = {}
                        callback = { helper() }
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured local helper references invoked through local aliases`() {
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
                        val callback = ::helper
                        val alias = callback
                        alias()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows configured local helper references passed to inline calls`() {
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

                inline fun immediately(block: () -> Unit) = block()

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        val callback = ::helper
                        immediately(callback)
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports shadowed configured local helper references`() {
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

                inline fun immediately(block: () -> Unit) = block()

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        fun helper() {
                            focusRequester.requestFocus()
                        }
                        val callback = ::helper
                        immediately {
                            val callback = {}
                            callback()
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows configured receiver references created inside invoked local lambda values`() {
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
                        val outer = {
                            val inner = focusRequester::requestFocus
                            inner()
                        }
                        outer()
                    }
                }
                """,
            ),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports configured receiver references passed as arguments`() {
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

                fun register(callback: () -> Unit) = Unit
                fun update() = Unit

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        register(focusRequester::requestFocus)
                        update()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver references wrapped in local function values`() {
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

                fun wrap(callback: () -> Unit): () -> Unit = {}

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        val callback = wrap(focusRequester::requestFocus)
                        callback()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver references wrapped in immediately invoked calls`() {
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

                fun wrap(callback: () -> Unit): () -> Unit = {}

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        wrap(focusRequester::requestFocus)()
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports configured receiver references shadowed by local function values`() {
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

                inline fun immediately(block: () -> Unit) = block()

                @Composable
                fun Example(focusRequester: FocusRequester) {
                    LaunchedEffect(Unit) {
                        val callback = focusRequester::requestFocus
                        immediately {
                            val callback = {}
                            callback()
                        }
                    }
                }
                """,
            ),
        )

        assertThat(findings).hasSize(1)
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
