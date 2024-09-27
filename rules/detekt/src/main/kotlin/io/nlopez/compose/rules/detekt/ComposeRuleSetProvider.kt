// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class ComposeRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = CUSTOM_RULE_SET_ID

    override fun instance(config: Config): RuleSet = RuleSet(
        CUSTOM_RULE_SET_ID,
        listOf(
            ComposableAnnotationNamingCheck(config),
            CompositionLocalAllowlistCheck(config),
            CompositionLocalNamingCheck(config),
            ContentEmitterReturningValuesCheck(config),
            ContentSlotReusedCheck(config),
            ContentTrailingLambdaCheck(config),
            DefaultsVisibilityCheck(config),
            LambdaParameterEventTrailingCheck(config),
            LambdaParameterInRestartableEffectCheck(config),
            Material2Check(config),
            ModifierClickableOrderCheck(config),
            ModifierComposableCheck(config),
            ModifierComposedCheck(config),
            ModifierMissingCheck(config),
            ModifierNamingCheck(config),
            ModifierNotUsedAtRootCheck(config),
            ModifierReusedCheck(config),
            ModifierWithoutDefaultCheck(config),
            MultipleContentEmittersCheck(config),
            MutableParametersCheck(config),
            MutableStateAutoboxingCheck(config),
            MutableStateParameterCheck(config),
            NamingCheck(config),
            ParameterNamingCheck(config),
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
        const val CUSTOM_RULE_SET_ID = "Compose"
    }
}
