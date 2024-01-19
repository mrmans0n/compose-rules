// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import java.util.*

fun <T> T.runIf(value: Boolean, block: T.() -> T): T = if (value) block() else this

fun <T, R> T.runIfNotNull(value: R?, block: T.(R) -> T): T = value?.let { block(it) } ?: this

fun <T, R> Sequence<T>.mapIf(condition: (T) -> Boolean, transform: (T) -> R): Sequence<R> =
    mapNotNull { if (condition(it)) transform(it) else null }

fun String?.matchesAnyOf(patterns: Sequence<Regex>): Boolean {
    if (isNullOrEmpty()) return false
    for (regex in patterns) {
        if (matches(regex)) return true
    }
    return false
}

fun String.toCamelCase() = split('_').joinToString(
    separator = "",
    transform = { original ->
        original.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    },
)

fun String.toSnakeCase() = replace(humps, "_").lowercase(Locale.getDefault())

private val humps by lazy(LazyThreadSafetyMode.NONE) { "(?<=.)(?=\\p{Upper})".toRegex() }
