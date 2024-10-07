package io.nlopez.compose.rules.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.rules.ModifierThenImplicitModifier
import io.nlopez.compose.rules.DetektRule

class ModifierThenImplicitModifierCheck(config: Config) :
    DetektRule(config),
    ComposeKtVisitor by ModifierThenImplicitModifier() {

    override val issue: Issue = Issue(
        id = "ModifierThenImplicitModifier",
        severity = Severity.CodeSmell,
        description = ModifierThenImplicitModifier.ModifierThenImplicitModifierErrorMessage,
        debt = Debt.FIVE_MINS,
    )
}
