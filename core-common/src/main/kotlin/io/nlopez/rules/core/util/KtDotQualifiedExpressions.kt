// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.util

import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

val KtDotQualifiedExpression.rootExpression: KtExpression
    get() {
        var current: KtExpression = receiverExpression
        while (current is KtDotQualifiedExpression) {
            current = current.receiverExpression
        }
        return current
    }
