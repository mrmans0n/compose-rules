// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

class ComposeRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSet.Id = RuleSet.Id(CUSTOM_RULE_SET_ID)

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        mapOf(
            RuleName("ComposableAnnotationNaming") to { config: Config -> ComposableAnnotationNamingCheck(config) },
            RuleName("ComposableNaming") to { config: Config -> NamingCheck(config) },
            RuleName("ComposableParamOrder") to { config: Config -> ParameterOrderCheck(config) },
            RuleName("CompositionLocalAllowlist") to { config: Config -> CompositionLocalAllowlistCheck(config) },
            RuleName("CompositionLocalNaming") to { config: Config -> CompositionLocalNamingCheck(config) },
            RuleName("ContentEmitterReturningValues") to
                { config: Config -> ContentEmitterReturningValuesCheck(config) },
            RuleName("ContentSlotReused") to { config: Config -> ContentSlotReusedCheck(config) },
            RuleName("ContentTrailingLambda") to { config: Config -> ContentTrailingLambdaCheck(config) },
            RuleName("DefaultsVisibility") to { config: Config -> DefaultsVisibilityCheck(config) },
            RuleName("LambdaParameterEventTrailing") to { config: Config -> LambdaParameterEventTrailingCheck(config) },
            RuleName("LambdaParameterInRestartableEffect") to { config: Config ->
                LambdaParameterInRestartableEffectCheck(
                    config,
                )
            },
            RuleName("Material2") to { config: Config -> Material2Check(config) },
            RuleName("ModifierClickableOrder") to { config: Config -> ModifierClickableOrderCheck(config) },
            RuleName("ModifierComposed") to { config: Config -> ModifierComposedCheck(config) },
            RuleName("ModifierMissing") to { config: Config -> ModifierMissingCheck(config) },
            RuleName("ModifierNaming") to { config: Config -> ModifierNamingCheck(config) },
            RuleName("ModifierNotUsedAtRoot") to { config: Config -> ModifierNotUsedAtRootCheck(config) },
            RuleName("ModifierReused") to { config: Config -> ModifierReusedCheck(config) },
            RuleName("ModifierWithoutDefault") to { config: Config -> ModifierWithoutDefaultCheck(config) },
            RuleName("MultipleEmitters") to { config: Config -> MultipleContentEmittersCheck(config) },
            RuleName("MutableParams") to { config: Config -> MutableParametersCheck(config) },
            RuleName("MutableStateAutoboxing") to { config: Config -> MutableStateAutoboxingCheck(config) },
            RuleName("MutableStateParam") to { config: Config -> MutableStateParameterCheck(config) },
            RuleName("ParameterNaming") to { config: Config -> ParameterNamingCheck(config) },
            RuleName("PreviewAnnotationNaming") to { config: Config -> PreviewAnnotationNamingCheck(config) },
            RuleName("PreviewNaming") to { config: Config -> PreviewNamingCheck(config) },
            RuleName("PreviewPublic") to { config: Config -> PreviewPublicCheck(config) },
            RuleName("RememberContentMissing") to { config: Config -> RememberContentMissingCheck(config) },
            RuleName("RememberMissing") to { config: Config -> RememberStateMissingCheck(config) },
            RuleName("StateParam") to { config: Config -> StateParameterCheck(config) },
            RuleName("UnstableCollections") to { config: Config -> UnstableCollectionsCheck(config) },
            RuleName("ViewModelForwarding") to { config: Config -> ViewModelForwardingCheck(config) },
            RuleName("ViewModelInjection") to { config: Config -> ViewModelInjectionCheck(config) },
        ),
    )

    private companion object {
        const val CUSTOM_RULE_SET_ID = "Compose"
    }
}
