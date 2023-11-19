// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class ComposeRuleSetProvider : RuleSetProviderV3(
    CustomRuleSetId,
) {

    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { ComposableAnnotationNamingCheck() },
        RuleProvider { CompositionLocalAllowlistCheck() },
        RuleProvider { CompositionLocalNamingCheck() },
        RuleProvider { ContentEmitterReturningValuesCheck() },
        RuleProvider { DefaultsVisibilityCheck() },
        RuleProvider { ModifierClickableOrderCheck() },
        RuleProvider { ModifierComposableCheck() },
        RuleProvider { ModifierMissingCheck() },
        RuleProvider { ModifierNamingCheck() },
        RuleProvider { ModifierNotUsedAtRootCheck() },
        RuleProvider { ModifierReusedCheck() },
        RuleProvider { ModifierWithoutDefaultCheck() },
        RuleProvider { MultipleContentEmittersCheck() },
        RuleProvider { MutableParametersCheck() },
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
        val CustomRuleSetId = RuleSetId("compose")
    }
}
