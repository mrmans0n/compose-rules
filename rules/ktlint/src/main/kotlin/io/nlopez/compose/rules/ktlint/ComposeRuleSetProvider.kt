// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class ComposeRuleSetProvider : RuleSetProviderV3(
    customRuleSetId,
) {

    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { ComposableAnnotationNamingCheck() },
        RuleProvider { CompositionLocalAllowlistCheck() },
        RuleProvider { CompositionLocalNamingCheck() },
        RuleProvider { ContentEmitterReturningValuesCheck() },
        RuleProvider { DefaultsVisibilityCheck() },
        RuleProvider { LambdaParameterInRestartableEffectCheck() },
        RuleProvider { Material2Check() },
        RuleProvider { ModifierClickableOrderCheck() },
        RuleProvider { ModifierComposableCheck() },
        RuleProvider { ModifierComposedCheck() },
        RuleProvider { ModifierMissingCheck() },
        RuleProvider { ModifierNamingCheck() },
        RuleProvider { ModifierNotUsedAtRootCheck() },
        RuleProvider { ModifierReusedCheck() },
        RuleProvider { ModifierWithoutDefaultCheck() },
        RuleProvider { MultipleContentEmittersCheck() },
        RuleProvider { MutableParametersCheck() },
        RuleProvider { MutableStateAutoboxingCheck() },
        RuleProvider { MutableStateParameterCheck() },
        RuleProvider { NamingCheck() },
        RuleProvider { ParameterOrderCheck() },
        RuleProvider { PreviewAnnotationNamingCheck() },
        RuleProvider { PreviewPublicCheck() },
        RuleProvider { RememberContentMissingCheck() },
        RuleProvider { RememberStateMissingCheck() },
        RuleProvider { UnstableCollectionsCheck() },
        RuleProvider { ViewModelForwardingCheck() },
        RuleProvider { ViewModelInjectionCheck() },
    )

    private companion object {
        val customRuleSetId = RuleSetId("compose")
    }
}
