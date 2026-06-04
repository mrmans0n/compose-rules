// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.name.FqName

internal fun <T> T.lintWithAnalysisApi(
    @Language("kotlin") content: String,
    vararg dependencyContents: String,
    allowCompilationErrors: Boolean = false,
): List<Finding> where T : Rule, T : RequiresAnalysisApi {
    val previousRuntime = ComposeFqNames.runtime
    val previousRuntimeSaveable = ComposeFqNames.runtimeSaveable
    val previousRuntimeRetain = ComposeFqNames.runtimeRetain

    return try {
        val fakeComposeFqName = FqName("com.example.compose.fake")
        ComposeFqNames.runtime = fakeComposeFqName
        ComposeFqNames.runtimeSaveable = fakeComposeFqName
        ComposeFqNames.runtimeRetain = fakeComposeFqName

        val environment = createEnvironment()
        lintWithContext(
            environment = environment,
            content = content,
            dependencyContents = arrayOf(fakeComposeRuntime(), *dependencyContents),
            allowCompilationErrors = allowCompilationErrors,
        )
    } finally {
        ComposeFqNames.runtime = previousRuntime
        ComposeFqNames.runtimeSaveable = previousRuntimeSaveable
        ComposeFqNames.runtimeRetain = previousRuntimeRetain
    }
}

@Language("kotlin")
internal fun codeWithFakeCompose(@Language("kotlin") code: String): String = """
    package com.example.compose.fake

    $code
""".trimIndent()

@Language("kotlin")
private fun fakeComposeRuntime(): String = codeWithFakeCompose(
    """
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE,
        AnnotationTarget.TYPE_PARAMETER,
        AnnotationTarget.PROPERTY_GETTER
    )
    annotation class Composable

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
    annotation class ReadOnlyComposable

    @Composable
    fun <T> remember(vararg keys: Any?, calculation: () -> T): T = calculation()

    @Composable
    fun <T> remember(calculation: () -> T): T = calculation()

    @Composable
    fun <T> rememberSaveable(vararg keys: Any?, calculation: () -> T): T = calculation()

    @Composable
    fun <T> rememberSaveable(key: String?, calculation: () -> T): T = calculation()

    @Composable
    fun <T> rememberSaveable(inputs: Array<Any?>, init: () -> T): T = init()

    @Composable
    fun <T> rememberSaveable(key: String?, init: () -> T): T = init()

    @Composable
    fun <T> rememberSaveable(calculation: () -> T): T = calculation()

    @Composable
    fun <T> retain(vararg keys: Any?, calculation: () -> T): T = calculation()

    @Composable
    fun <T> retain(calculation: () -> T): T = calculation()

    interface State<T> {
        val value: T
    }

    @Composable
    fun <T> rememberUpdatedState(newValue: T): State<T> = object : State<T> {
        override val value: T get() = newValue
    }

    @Composable
    operator fun <T> State<T>.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = value
    """,
)
