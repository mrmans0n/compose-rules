// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class ComposeRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = CustomRuleSetId

    override fun instance(config: Config): RuleSet = RuleSet(
        CustomRuleSetId,
        listOf(
            ComposableAnnotationNamingCheck(config),
            CompositionLocalAllowlistCheck(config),
            CompositionLocalNamingCheck(config),
            ContentEmitterReturningValuesCheck(config),
            DefaultsVisibilityCheck(config),
            ModifierClickableOrderCheck(config),
            ModifierComposableCheck(config),
            ModifierMissingCheck(config),
            ModifierNamingCheck(config),
            ModifierNotUsedAtRootCheck(config),
            ModifierReusedCheck(config),
            ModifierWithoutDefaultCheck(config),
            MultipleContentEmittersCheck(config),
            MutableParametersCheck(config),
            NamingCheck(config),
            ParameterOrderCheck(config),
            PreviewAnnotationNamingCheck(config),
            PreviewPublicCheck(config),
            RememberContentMissingCheck(config),
            RememberStateMissingCheck(config),
            UnstableCollectionsCheck(config),
            ViewModelForwardingCheck(config),
            ViewModelInjectionCheck(config),
        ),
    )

    private companion object {
        const val CustomRuleSetId = "Compose"
    }
}
