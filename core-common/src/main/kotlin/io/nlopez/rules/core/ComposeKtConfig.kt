// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core

interface ComposeKtConfig {
    fun getInt(key: String, default: Int): Int
    fun getString(key: String, default: String?): String?
    fun getList(key: String, default: List<String>): List<String>
    fun getSet(key: String, default: Set<String>): Set<String>
    fun getBoolean(key: String, default: Boolean): Boolean
}
