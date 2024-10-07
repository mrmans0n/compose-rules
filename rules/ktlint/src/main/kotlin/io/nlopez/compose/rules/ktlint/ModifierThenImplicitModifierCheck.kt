package io.nlopez.compose.rules.ktlint

import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ModifierThenImplicitModifier
import io.nlopez.compose.rules.KtlintRule

class ModifierThenImplicitModifier :
    KtlintRule("compose:modifier-then-implicit-modifier"),
    ComposeKtVisitor by ModifierThenImplicitModifier()
