// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.rules.core.detekt.DetektRule
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.reflections.Reflections

class ComposeRuleSetProviderTest {

    private val ruleSetProvider = ComposeRuleSetProvider()
    private val ruleSet = ruleSetProvider.instance(Config.empty)

    @Test
    fun `ensure all rules in the package are represented in the ruleset`() {
        val reflections = Reflections(ruleSetProvider.javaClass.packageName)
        val ruleClassesInPackage = reflections.getSubTypesOf(DetektRule::class.java)
        val ruleClassesInRuleSet = ruleSet.rules.filterIsInstance<DetektRule>().map { it::class.java }.toSet()
        assertThat(ruleClassesInRuleSet).containsExactlyInAnyOrderElementsOf(ruleClassesInPackage)
    }
}
