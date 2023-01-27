// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION")

package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2

class ComposeRuleSetProvider : RuleSetProviderV2(
    CustomRuleSetId,
    RuleSetAbout,
) {

    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { ComposeCompositionLocalAllowlistCheck() },
        RuleProvider { ComposeCompositionLocalNamingCheck() },
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
        private val RuleSetAbout = About(
            maintainer = "Nacho Lopez",
            description = "Static checks to aid with a healthy adoption of Jetpack Compose",
            license = "Apache License, Version 2.0",
            repositoryUrl = "https://github.com/mrmans0n/compose-rules/",
            issueTrackerUrl = "https://github.com/mrmans0n/compose-rules/issues",
        )
        const val CustomRuleSetId = "compose"
    }
}
