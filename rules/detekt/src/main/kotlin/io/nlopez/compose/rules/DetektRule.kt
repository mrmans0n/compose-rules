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
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.runIf
import org.jetbrains.kotlin.com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction

abstract class DetektRule(
    config: Config = Config.empty,
    description: String,
) : Rule(config, description), ComposeKtVisitor {

    private val composeKtConfig: ComposeKtConfig by lazy { DetektComposeKtConfig(config) }

    private val emitter: Emitter = Emitter { element, message, canBeAutoCorrected ->
        // Grab the named element if there were any, otherwise fall back to the whole PsiElement
        val finalElement = element.runIf(element is PsiNameIdentifierOwner) {
            (this as PsiNameIdentifierOwner).nameIdentifier!!
        }
        val finding = when {
            canBeAutoCorrected -> CorrectableCodeSmell(
                entity = Entity.from(finalElement, Location.from(finalElement)),
                message = message,
                autoCorrectEnabled = autoCorrect,
            )

            else -> CodeSmell(
                entity = Entity.from(finalElement, Location.from(finalElement)),
                message = message,
            )
        }
        report(finding)
    }

    override fun visit(root: KtFile) {
        super.visit(root)
        visitFile(root, autoCorrect, emitter, composeKtConfig)
    }

    override fun visitClass(klass: KtClass) {
        super<Rule>.visitClass(klass)
        visitClass(klass, autoCorrect, emitter, composeKtConfig)
    }

    override fun visitKtElement(element: KtElement) {
        super.visitKtElement(element)
        when (element) {
            is KtFunction -> {
                visitFunction(element, autoCorrect, emitter, composeKtConfig)
                if (element.isComposable) {
                    visitComposable(element, autoCorrect, emitter, composeKtConfig)
                }
            }
        }
    }
}
