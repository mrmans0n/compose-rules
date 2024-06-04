// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.core

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction

interface ComposeKtVisitor {

    fun visitFunction(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {}

    fun visitComposable(function: KtFunction, emitter: Emitter, config: ComposeKtConfig) {}

    fun visitClass(clazz: KtClass, emitter: Emitter, config: ComposeKtConfig) {}

    fun visitFile(file: KtFile, emitter: Emitter, config: ComposeKtConfig) {}
}
