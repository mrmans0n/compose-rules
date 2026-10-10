// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withParentNamed
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RulesHaveTestsTest {

    @ParameterizedTest
    @ValueSource(strings = ["KtlintRule", "DetektRule"])
    fun `ensure all rules have a unit test`(baseClass: String) {
        Konsist.scopeFromProduction()
            .classes()
            .withParentNamed(baseClass)
            .assertTrue { clazz ->
                // Rules share names across ktlint and detekt, so the test must live in the same module
                clazz.testClasses { it.hasNameContaining(clazz.name) && it.moduleName == clazz.moduleName }.isNotEmpty()
            }
    }
}
