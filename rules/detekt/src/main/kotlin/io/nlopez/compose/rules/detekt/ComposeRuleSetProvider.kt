// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class ComposeRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSet.Id(CUSTOM_RULE_SET_ID)

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::ComposableAnnotationNamingCheck,
            ::CompositionLocalAllowlistCheck,
            ::CompositionLocalNamingCheck,
            ::ContentEmitterReturningValuesCheck,
            ::ContentTrailingLambdaCheck,
            ::DefaultsVisibilityCheck,
            ::LambdaParameterInRestartableEffectCheck,
            ::Material2Check,
            ::ModifierClickableOrderCheck,
            ::ModifierComposableCheck,
            ::ModifierComposedCheck,
            ::ModifierMissingCheck,
            ::ModifierNamingCheck,
            ::ModifierNotUsedAtRootCheck,
            ::ModifierReusedCheck,
            ::ModifierWithoutDefaultCheck,
            ::MultipleContentEmittersCheck,
            ::MutableParametersCheck,
            ::MutableStateAutoboxingCheck,
            ::MutableStateParameterCheck,
            ::NamingCheck,
            ::ParameterNamingCheck,
            ::ParameterOrderCheck,
            ::PreviewAnnotationNamingCheck,
            ::PreviewPublicCheck,
            ::RememberContentMissingCheck,
            ::RememberStateMissingCheck,
            ::UnstableCollectionsCheck,
            ::ViewModelForwardingCheck,
            ::ViewModelInjectionCheck,
        ),
    )

    private companion object {
        const val CUSTOM_RULE_SET_ID = "Compose"
    }
}
