// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction

interface ComposeKtVisitor {

    fun visitFunction(function: KtFunction, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {}

    fun visitComposable(function: KtFunction, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {}

    fun visitClass(clazz: KtClass, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {}

    fun visitFile(file: KtFile, autoCorrect: Boolean, emitter: Emitter, config: ComposeKtConfig) {}
}
