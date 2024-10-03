// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAllParentsOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.KtlintRule
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

class ComposeRuleSetProviderTest {

    private val ruleSetProvider = ComposeRuleSetProvider()
    private val ruleClassesInPackage = Reflections(ruleSetProvider.javaClass.packageName)
        .getSubTypesOf(KtlintRule::class.java)

    @Test
    fun `ensure all rules in the package are represented in the ruleset`() {
        val ruleSet = ruleSetProvider.getRuleProviders()
        val ruleClassesInRuleSet = ruleSet.map { it.createNewRuleInstance() }
            .filterIsInstance<KtlintRule>()
            .map { it::class.java }
            .toSet()
        assertThat(ruleClassesInRuleSet).containsExactlyInAnyOrderElementsOf(ruleClassesInPackage)
    }

    @Test
    fun `ensure all rules in the package are listed in alphabetical order`() {
        val isOrdered = ruleSetProvider.getRuleProviders()
            .filterIsInstance<KtlintRule>()
            .asSequence()
            .map { it::class.java.simpleName }
            .zipWithNext { a, b -> a <= b }
            .all { it }
        assertThat(isOrdered)
            .describedAs("ComposeRuleSetProvider should have the rules in alphabetical order")
            .isTrue()
    }

    @Test
    fun `ensure all available rules have a ktlint rule`() {
        val ktlintRuleNames = ruleClassesInPackage.map { it.simpleName }

        val commonRulesReflections = Reflections(
            ConfigurationBuilder()
                .setClassLoaders(arrayOf(ComposeKtVisitor::class.java.classLoader))
                .setScanners(Scanners.SubTypes),
        )
        val ruleNames = commonRulesReflections.getSubTypesOf(ComposeKtVisitor::class.java).map { it.simpleName }

        for (ruleName in ruleNames) {
            assertThat(ktlintRuleNames)
                .describedAs { "$ruleName should have a ktlint rule named ${ruleName}Check" }
                .contains("${ruleName}Check")
        }
    }

    @Test
    fun `ensure all ktlint rules have a unit test`() {
        Konsist.scopeFromProduction()
            .classes()
            .withAllParentsOf(KtlintRule::class)
            .assertTrue { clazz ->
                clazz.testClasses { it.hasNameContaining(clazz.name) }.isNotEmpty()
            }
    }
}
