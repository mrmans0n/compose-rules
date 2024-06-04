// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.CorrectableCodeSmell
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Location
import io.gitlab.arturbosch.detekt.api.Rule
import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Decision
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.runIf
import org.jetbrains.kotlin.com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction

abstract class DetektRule(config: Config = Config.empty) :
    Rule(config),
    ComposeKtVisitor {

    private val config: ComposeKtConfig by lazy { DetektComposeKtConfig(this) }

    private val emitter: Emitter = Emitter { element, message, canBeAutoCorrected ->
        // Grab the named element if there were any, otherwise fall back to the whole PsiElement
        val finalElement = element.runIf(element is PsiNameIdentifierOwner) {
            (this as PsiNameIdentifierOwner).nameIdentifier!!
        }
        val finding = when {
            canBeAutoCorrected -> CorrectableCodeSmell(
                issue = issue,
                entity = Entity.from(finalElement, Location.from(finalElement)),
                message = message,
                autoCorrectEnabled = autoCorrect,
            )

            else -> CodeSmell(
                issue = issue,
                entity = Entity.from(finalElement, Location.from(finalElement)),
                message = message,
            )
        }
        report(finding)

        when {
            this@DetektRule.autoCorrect && canBeAutoCorrected -> Decision.Fix
            else -> Decision.Ignore
        }
    }

    override fun visit(root: KtFile) {
        super.visit(root)
        visitFile(root, emitter, config)
    }

    override fun visitClass(klass: KtClass) {
        super<Rule>.visitClass(klass)
        visitClass(klass, emitter, config)
    }

    override fun visitKtElement(element: KtElement) {
        super.visitKtElement(element)
        when (element) {
            is KtFunction -> {
                visitFunction(element, emitter, config)
                if (element.isComposable) {
                    visitComposable(element, emitter, config)
                }
            }
        }
    }
}
