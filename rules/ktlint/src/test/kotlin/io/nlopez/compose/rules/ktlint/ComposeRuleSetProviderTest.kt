// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.nlopez.rules.core.ktlint.TwitterKtlintRule
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.reflections.Reflections

class ComposeRuleSetProviderTest {

    private val ruleSetProvider = ComposeRuleSetProvider()
    private val ruleClassesInPackage = Reflections(ruleSetProvider.javaClass.packageName)
        .getSubTypesOf(TwitterKtlintRule::class.java)

    @Test
    fun `ensure all rules in the package are represented in the ruleset`() {
        val ruleSet = ruleSetProvider.getRuleProviders()
        val ruleClassesInRuleSet = ruleSet.map { it.createNewRuleInstance() }
            .filterIsInstance<TwitterKtlintRule>()
            .map { it::class.java }
            .toSet()
        assertThat(ruleClassesInRuleSet).containsExactlyInAnyOrderElementsOf(ruleClassesInPackage)
    }
}
