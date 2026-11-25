// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import io.github.ktlint.core.cli.ruleset.core.api.RuleSetV2Provider
import io.github.ktlint.core.rule.engine.core.api.RuleSetId
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider

class ComposeRuleSetProvider :
    RuleSetV2Provider(
        customRuleSetId,
    ) {

    override fun getRuleProviders(): Set<RuleV2Provider> = setOf(
        RuleV2Provider { ComposableAnnotationNamingCheck() },
        RuleV2Provider { ComposableNestingDepthCheck() },
        RuleV2Provider { CompositionLocalAllowlistCheck() },
        RuleV2Provider { CompositionLocalNamingCheck() },
        RuleV2Provider { ContentEmitterReturningValuesCheck() },
        RuleV2Provider { ContentSlotReusedCheck() },
        RuleV2Provider { ContentTrailingLambdaCheck() },
        RuleV2Provider { DefaultsVisibilityCheck() },
        RuleV2Provider { LambdaParameterEventTrailingCheck() },
        RuleV2Provider { LambdaParameterInRestartableEffectCheck() },
        RuleV2Provider { Material2Check() },
        RuleV2Provider { ModifierClickableOrderCheck() },
        RuleV2Provider { ModifierComposedCheck() },
        RuleV2Provider { ModifierMissingCheck() },
        RuleV2Provider { ModifierNamingCheck() },
        RuleV2Provider { ModifierNotUsedAtRootCheck() },
        RuleV2Provider { ModifierReusedCheck() },
        RuleV2Provider { ModifierShouldBeForwardedToEveryBranchCheck() },
        RuleV2Provider { ModifierWithoutDefaultCheck() },
        RuleV2Provider { MultipleContentEmittersCheck() },
        RuleV2Provider { MutableParametersCheck() },
        RuleV2Provider { MutableStateAutoboxingCheck() },
        RuleV2Provider { MutableStateParameterCheck() },
        RuleV2Provider { StateParameterCheck() },
        RuleV2Provider { NamingCheck() },
        RuleV2Provider { ParameterNamingCheck() },
        RuleV2Provider { ParameterOrderCheck() },
        RuleV2Provider { PreviewAnnotationNamingCheck() },
        RuleV2Provider { PreviewNamingCheck() },
        RuleV2Provider { PreviewPublicCheck() },
        RuleV2Provider { RememberContentMissingCheck() },
        RuleV2Provider { RememberStateMissingCheck() },
        RuleV2Provider { UnstableCollectionsCheck() },
        RuleV2Provider { ViewModelForwardingCheck() },
        RuleV2Provider { ViewModelInjectionCheck() },
    )

    private companion object {
        val customRuleSetId = RuleSetId("compose")
    }
}
