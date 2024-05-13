// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.util.toSnakeCase

/**
 * Manages the configuration for ktlint rules. In ktlint, configs are typically in snake case, while in the
 * whole project and in detekt they are camel case, so this class will convert all camel case keys to snake case,
 * and add a "compose_" prefix to all of them.
 * Results will be memoized as well, as config shouldn't be changing during the lifetime of a rule.
 */
internal class KtlintComposeKtConfig(
    private val properties: EditorConfig,
    private val editorConfigProperties: Set<EditorConfigProperty<*>>,
) : ComposeKtConfig {
    private val cache = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getValueAsOrPut(key: String, value: () -> T?): T? = cache.getOrPut(key) { value() } as? T

    override fun getInt(key: String, default: Int): Int = getValueAsOrPut(key) { find<String>(key)?.toInt() } ?: default

    override fun getString(key: String, default: String?): String? = getValueAsOrPut(key) { find(key) } ?: default

    override fun getList(key: String, default: List<String>): List<String> = getValueAsOrPut(key) {
        find<String>(key)?.split(',', ';')?.map { it.trim() }
    } ?: default

    override fun getSet(key: String, default: Set<String>): Set<String> =
        getValueAsOrPut(key) { getList(key, default.toList()).toSet() } ?: default

    override fun getBoolean(key: String, default: Boolean): Boolean = getValueAsOrPut(key) { find(key) } ?: default

    private fun <T> find(key: String): T? {
        val name = ktlintKey(key)
        @Suppress("UNCHECKED_CAST")
        return editorConfigProperties.filter { it.name == name }.map { properties[it] }.firstOrNull() as T
    }

    private companion object {
        private fun ktlintKey(key: String): String = "compose_${key.toSnakeCase()}"
    }
}
