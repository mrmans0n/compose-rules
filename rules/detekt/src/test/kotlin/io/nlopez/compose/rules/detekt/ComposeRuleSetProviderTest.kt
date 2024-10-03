// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.yamlMap
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAllParentsOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.gitlab.arturbosch.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.io.File

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

    @Test
    fun `ensure all rules in the package are listed in alphabetical order`() {
        val isOrdered = ruleSet.rules
            .filterIsInstance<DetektRule>()
            .asSequence()
            .map { it::class.java.simpleName }
            .zipWithNext { a, b -> a <= b }.all { it }
        assertThat(isOrdered)
            .describedAs("ComposeRuleSetProvider should have the rules in alphabetical order")
            .isTrue()
    }

    @Test
    fun `ensure all rules in the package are listed in the default config`() {
        val rules = ruleSet.rules
            .asSequence()
            .filterIsInstance<DetektRule>()
            .map { it.ruleId to it.isOptIn }

        val optIn = rules.associate { it.first to it.second }

        val ruleIds = rules.map { it.first }.toSet()

        // Grab the config file and read it
        val defaultConfig = javaClass.classLoader.getResource("config/config.yml")
        assertThat(defaultConfig).isNotNull()
        requireNotNull(defaultConfig)
        val file = File(defaultConfig.toURI())

        // Parse the config file
        val parsed = Yaml.default.parseToYamlNode(file.readText())
        val configRules = parsed.yamlMap.get<YamlMap>("Compose")?.entries
        assertThat(configRules).isNotNull()
        requireNotNull(configRules)

        // Check that all rules in the package are listed in the config
        val configRuleIds = configRules.keys.map { it.content }
        assertThat(configRuleIds)
            .describedAs { "all rules in the ruleset are listed in the default config" }
            .containsExactlyInAnyOrderElementsOf(ruleIds)

        // Check that all rules in the package match the active config
        val ruleIdsWithActiveConfig = configRules
            .filter { it.value.yamlMap.getKey("active") != null }
            .mapKeys { it.key.content }
            .mapValues { it.value.yamlMap.get<YamlScalar>("active")?.toBoolean() }

        assertThat(ruleIdsWithActiveConfig.keys)
            .describedAs { "all rules in the ruleset are defining whether they are active" }
            .isEqualTo(ruleIds)

        // Make sure the active config matches the opt-in config of the rule
        for ((key, value) in ruleIdsWithActiveConfig) {
            assertThat(optIn).containsKey(key)
            val isOptIn = optIn[key]!!

            // If it's opt-in, active should be false
            val shouldBeActive = !isOptIn

            assertThat(value)
                .describedAs { "Rule $key must be active: $shouldBeActive in the config" }
                .isEqualTo(shouldBeActive)
        }
    }

    @Test
    fun `ensure all available rules have a detekt rule`() {
        val detektRulesReflections = Reflections(ruleSetProvider.javaClass.packageName)
        val detektRuleNames = detektRulesReflections.getSubTypesOf(DetektRule::class.java).map { it.simpleName }

        val commonRulesReflections = Reflections(
            ConfigurationBuilder()
                .setClassLoaders(arrayOf(ComposeKtVisitor::class.java.classLoader))
                .setScanners(Scanners.SubTypes),
        )
        val ruleNames = commonRulesReflections.getSubTypesOf(ComposeKtVisitor::class.java).map { it.simpleName }

        for (ruleName in ruleNames) {
            assertThat(detektRuleNames)
                .describedAs { "$ruleName should have a detekt rule named ${ruleName}Check" }
                .contains("${ruleName}Check")
        }
    }

    @Test
    fun `ensure all detekt rules have a unit test`() {
        Konsist.scopeFromProduction()
            .classes()
            .withAllParentsOf(DetektRule::class)
            .assertTrue { clazz ->
                clazz.testClasses { it.hasNameContaining(clazz.name) }.isNotEmpty()
            }
    }
}
