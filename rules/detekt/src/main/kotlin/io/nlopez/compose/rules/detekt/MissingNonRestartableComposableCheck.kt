// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(KaExperimentalApi::class)

package io.nlopez.compose.rules.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import io.nlopez.compose.core.util.definedInInterface
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.components.builtinTypes
import org.jetbrains.kotlin.analysis.api.components.evaluate
import org.jetbrains.kotlin.analysis.api.components.expressionType
import org.jetbrains.kotlin.analysis.api.components.resolveCall
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.components.semanticallyEquals
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.net.URI

/** Reports pass-through composables that can avoid generating their own restart boundary. */
class MissingNonRestartableComposableCheck(config: Config) :
    Rule(
        config,
        "Pass-through composables should consider using @NonRestartableComposable.",
        URI(
            "https://mrmans0n.github.io/compose-rules/rules/#mark-pass-through-composables-as-non-restartable",
        ),
    ),
    RequiresAnalysisApi {

    override val ruleName: RuleName = RuleName("MissingNonRestartableComposable")

    private val ignoresPreviews = config.valueOrDefault("ignoresPreviews", true)

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isComposable()) return
        if (!function.returnsUnit()) return
        if (function.isLocal || function.definedInInterface) return
        if (function.hasIneligibleModifier()) return
        if (ignoresPreviews && function.isPreview()) return
        if (function.hasIneligibleAnnotation()) return
        if (function.valueParameters.any { parameter ->
                parameter.defaultValue?.isSafeValue(function.valueParameters) == false
            }
        ) {
            return
        }

        checkPassThrough(function, function.bodyExpression, function.valueParameters)
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        super.visitPropertyAccessor(accessor)
        if (!accessor.isGetter || !accessor.isComposable()) return
        if (!accessor.returnsUnit()) return
        if (accessor.property.isLocal) return
        if (accessor.property.getStrictParentOfType<KtClass>()?.isInterface() == true) return
        if (accessor.hasIneligibleModifier() || accessor.property.hasIneligibleModifier()) return
        if (accessor.hasIneligibleAnnotation()) return

        checkPassThrough(accessor, accessor.bodyExpression, emptyList())
    }

    private fun checkPassThrough(
        declaration: KtElement,
        bodyExpression: KtExpression?,
        parameters: List<KtParameter>,
    ) {
        val directCall = bodyExpression.singleBodyExpression()?.directCall() ?: return
        val call = directCall.call
        if (directCall.receiver?.isSafeReceiver(parameters) == false) return
        if (!call.isComposableCall()) return
        if (!call.returnsUnit()) return
        if (call.hasIneligibleContract()) return
        if (call.valueArguments.any { argument ->
                argument.getSpreadElement() != null ||
                    argument.getArgumentExpression()?.isSafeValue(parameters) != true
            }
        ) {
            return
        }

        report(
            Finding(
                Entity.from(declaration),
                MissingNonRestartableComposable,
            ),
        )
    }

    private fun KtNamedFunction.returnsUnit(): Boolean = runCatching {
        analyze(this) {
            symbol.returnType.semanticallyEquals(builtinTypes.unit)
        }
    }.getOrDefault(false)

    private fun KtPropertyAccessor.returnsUnit(): Boolean = runCatching {
        analyze(this) {
            symbol.returnType.semanticallyEquals(builtinTypes.unit)
        }
    }.getOrDefault(false)

    private fun KtCallExpression.returnsUnit(): Boolean = runCatching {
        analyze(this) {
            expressionType?.semanticallyEquals(builtinTypes.unit) == true
        }
    }.getOrDefault(false)

    private fun KtNamedFunction.hasIneligibleAnnotation(): Boolean = runCatching {
        analyze(this) {
            val annotationClassIds = ineligibleAnnotationClassIds()
            symbol.annotations.any { annotation -> annotation.classId in annotationClassIds }
        }
    }.getOrDefault(false)

    private fun KtNamedFunction.isPreview(): Boolean = runCatching {
        analyze(this) {
            fun KaAnnotation.isPreview(visitedClassIds: Set<ClassId> = emptySet()): Boolean {
                val classId = classId ?: return false
                if (classId == ClassId.topLevel(ComposeFqNames.Preview)) return true
                if (classId in visitedClassIds) return false
                return findClass(classId)?.annotations?.any { annotation ->
                    annotation.isPreview(visitedClassIds + classId)
                } == true
            }

            symbol.annotations.any { annotation -> annotation.isPreview() }
        }
    }.getOrDefault(false)

    private fun KtPropertyAccessor.hasIneligibleAnnotation(): Boolean = runCatching {
        analyze(this) {
            val annotationClassIds = ineligibleAnnotationClassIds()
            symbol.annotations.any { annotation -> annotation.classId in annotationClassIds }
        }
    }.getOrDefault(false)

    private fun KtCallExpression.hasIneligibleContract(): Boolean = runCatching {
        analyze(this) {
            val call = resolveCall() ?: return@analyze true
            val symbol = call.signature.symbol
            if (symbol is KaNamedFunctionSymbol && symbol.isBuiltinFunctionInvoke) {
                return@analyze true
            }
            val annotationClassIds = ineligibleAnnotationClassIds()
            if (symbol.annotations.any { annotation -> annotation.classId in annotationClassIds }) {
                return@analyze true
            }

            symbol is KaNamedFunctionSymbol &&
                (
                    symbol.isInline ||
                        symbol.isOverride ||
                        symbol.isExpect ||
                        symbol.isExternal ||
                        symbol.isTailRec ||
                        symbol.modality != KaSymbolModality.FINAL
                    )
        }
    }.getOrDefault(true)

    private fun ineligibleAnnotationClassIds(): Set<ClassId> = setOf(
        ClassId.topLevel(ComposeFqNames.NonRestartableComposable),
        ClassId.topLevel(ComposeFqNames.NonSkippableComposable),
        ClassId.topLevel(ComposeFqNames.ReadOnlyComposable),
        ClassId.topLevel(ComposeFqNames.ExplicitGroupsComposable),
    )

    private fun KtExpression?.singleBodyExpression(): KtExpression? {
        val expression = this?.blockExpressionsOrSingle()?.singleOrNull() as? KtExpression ?: return null
        return KtPsiUtil.safeDeparenthesize(expression)
    }

    private fun KtModifierListOwner.hasIneligibleModifier(): Boolean = IneligibleModifiers.any(::hasModifier)

    private fun KtExpression.directCall(): DirectCall? = when (this) {
        is KtCallExpression -> DirectCall(this, null)

        is KtDotQualifiedExpression ->
            (selectorExpression as? KtCallExpression)?.let { call -> DirectCall(call, receiverExpression) }

        else -> null
    }

    private fun KtExpression.isSafeReceiver(parameters: List<KtParameter>): Boolean =
        when (val expression = KtPsiUtil.safeDeparenthesize(this)) {
            is KtThisExpression -> true

            is KtNameReferenceExpression ->
                expression.resolvesToParameter(parameters) || expression.resolvesToQualifier()

            is KtDotQualifiedExpression -> expression.isTypeOrPackageQualifier()

            else -> false
        }

    private fun KtDotQualifiedExpression.isTypeOrPackageQualifier(): Boolean =
        receiverExpression.isTypeOrPackageQualifier() &&
            (selectorExpression as? KtNameReferenceExpression)?.resolvesToQualifier() == true

    private fun KtExpression.isTypeOrPackageQualifier(): Boolean =
        when (val expression = KtPsiUtil.safeDeparenthesize(this)) {
            is KtNameReferenceExpression -> expression.resolvesToQualifier()
            is KtDotQualifiedExpression -> expression.isTypeOrPackageQualifier()
            else -> false
        }

    private fun KtNameReferenceExpression.resolvesToQualifier(): Boolean = runCatching {
        analyze(this) {
            when (mainReference.resolveToSymbol()) {
                is KaClassLikeSymbol, is KaEnumEntrySymbol, is KaPackageSymbol -> true
                else -> false
            }
        }
    }.getOrDefault(false)

    private fun KtNameReferenceExpression.resolvesToParameter(parameters: List<KtParameter>): Boolean = runCatching {
        analyze(this) {
            mainReference.resolveToSymbol()?.sourcePsiSafe<KtParameter>() in parameters
        }
    }.getOrDefault(false)

    private fun KtExpression.isSafeValue(parameters: List<KtParameter>): Boolean {
        val expression = KtPsiUtil.safeDeparenthesize(this)
        return runCatching {
            analyze(expression) {
                if (expression.evaluate() != null) return@analyze true
                if (expression is KtThisExpression) return@analyze true

                val reference = when (expression) {
                    is KtNameReferenceExpression -> expression
                    is KtDotQualifiedExpression -> expression.selectorExpression as? KtNameReferenceExpression
                    else -> null
                } ?: return@analyze false

                when (val symbol = reference.mainReference.resolveToSymbol()) {
                    is KaEnumEntrySymbol -> true
                    is KaNamedClassSymbol -> symbol.classKind.isObject
                    else -> symbol?.sourcePsiSafe<KtParameter>() in parameters
                }
            }
        }.getOrDefault(false)
    }

    internal companion object {
        val MissingNonRestartableComposable = """
            This @Composable declaration only delegates to another composable. Consider marking it @NonRestartableComposable when its own restart and skip boundary is not useful.

            See https://mrmans0n.github.io/compose-rules/rules/#mark-pass-through-composables-as-non-restartable for more information.
        """.trimIndent()
    }
}

private data class DirectCall(val call: KtCallExpression, val receiver: KtExpression?)

private val IneligibleModifiers = setOf(
    KtTokens.INLINE_KEYWORD,
    KtTokens.OPEN_KEYWORD,
    KtTokens.OVERRIDE_KEYWORD,
    KtTokens.ABSTRACT_KEYWORD,
    KtTokens.EXPECT_KEYWORD,
    KtTokens.ACTUAL_KEYWORD,
    KtTokens.EXTERNAL_KEYWORD,
    KtTokens.TAILREC_KEYWORD,
)
