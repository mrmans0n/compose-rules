// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION")

package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class ComposeRuleSetProvider : RuleSetProviderV3(
    CustomRuleSetId,
) {

    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { ComposeCompositionLocalAllowlistCheck() },
        RuleProvider { ComposeContentEmitterReturningValuesCheck() },
        RuleProvider { ComposeModifierComposableCheck() },
        RuleProvider { ComposeModifierMissingCheck() },
        RuleProvider { ComposeModifierReusedCheck() },
        RuleProvider { ComposeModifierWithoutDefaultCheck() },
        RuleProvider { ComposeMultipleContentEmittersCheck() },
        RuleProvider { ComposeMutableParametersCheck() },
        RuleProvider { ComposeNamingCheck() },
        RuleProvider { ComposeParameterOrderCheck() },
        RuleProvider { ComposePreviewNamingCheck() },
        RuleProvider { ComposePreviewPublicCheck() },
        RuleProvider { ComposeRememberMissingCheck() },
        RuleProvider { ComposeUnstableCollectionsCheck() },
        RuleProvider { ComposeViewModelForwardingCheck() },
        RuleProvider { ComposeViewModelInjectionCheck() },
    )

    private companion object {
        val CustomRuleSetId = RuleSetId("compose")
    }
}
