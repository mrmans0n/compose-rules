// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.DetektRule
import io.nlopez.compose.rules.LambdaParameterInRestartableEffect
import java.net.URI

class LambdaParameterInRestartableEffectCheck(config: Config) :
    DetektRule(
        config = config,
        description = "Lambda parameters should not be used inside restartable effects",
        url = URI("https://mrmans0n.github.io/compose-rules/rules/#lambda-parameter-in-restartable-effect"),
    ),
    ComposeKtVisitor by LambdaParameterInRestartableEffect()
