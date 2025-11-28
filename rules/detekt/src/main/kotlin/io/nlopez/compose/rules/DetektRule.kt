// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import com.intellij.psi.PsiNameIdentifierOwner
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Decision
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.util.isComposable
import io.nlopez.compose.core.util.runIf
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.net.URI

abstract class DetektRule(
    config: Config = Config.empty,
    description: String,
    url: URI = URI("https://mrmans0n.github.io/compose-rules/"),
) : Rule(config, description, url),
    ComposeKtVisitor {

    private val composeConfig: DetektComposeKtConfig by lazy { DetektComposeKtConfig(config) }

    private val emitter: Emitter = Emitter { element, message, canBeAutoCorrected ->
        // Create entity and finding using the detekt 2.0 API
        @Suppress("UNCHECKED_CAST")
        val psiElement = element as com.intellij.psi.PsiElement

        // Use Entity.atName() for named declarations to report at the name identifier location
        val entity = when (psiElement) {
            is KtNamedDeclaration -> Entity.atName(psiElement)
            else -> Entity.from(psiElement)
        }

        val finding = Finding(
            entity = entity,
            message = message,
            references = emptyList(),
            suppressReasons = emptyList(),
        )

        report(finding)

        when {
            this@DetektRule.autoCorrect && canBeAutoCorrected -> Decision.Fix
            else -> Decision.Ignore
        }
    }

    override fun visit(root: KtFile) {
        super.visit(root)
        visitFile(root, emitter, composeConfig)
    }

    override fun visitClass(klass: KtClass) {
        super<Rule>.visitClass(klass)
        visitClass(klass, emitter, composeConfig)
    }

    override fun visitKtElement(element: KtElement) {
        super.visitKtElement(element)
        when (element) {
            is KtFunction -> {
                visitFunction(element, emitter, composeConfig)
                if (element.isComposable) {
                    visitComposable(element, emitter, composeConfig)
                }
            }
        }
    }
}
