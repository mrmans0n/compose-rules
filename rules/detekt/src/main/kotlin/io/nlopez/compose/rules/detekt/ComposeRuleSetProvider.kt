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
            ComposeCompositionLocalAllowlistCheck(config),
            ComposeContentEmitterReturningValuesCheck(config),
            ComposeDefaultsVisibilityCheck(config),
            ComposeModifierComposableCheck(config),
            ComposeModifierMissingCheck(config),
            ComposeModifierNamingCheck(config),
            ComposeModifierReusedCheck(config),
            ComposeModifierWithoutDefaultCheck(config),
            ComposeMultipleContentEmittersCheck(config),
            ComposeMutableParametersCheck(config),
            ComposeNamingCheck(config),
            ComposeParameterOrderCheck(config),
            ComposePreviewNamingCheck(config),
            ComposePreviewPublicCheck(config),
            ComposeRememberMissingCheck(config),
            ComposeUnstableCollectionsCheck(config),
            ComposeViewModelForwardingCheck(config),
            ComposeViewModelInjectionCheck(config),
        ),
    )

    private companion object {
        const val CustomRuleSetId = "Compose"
    }
}
