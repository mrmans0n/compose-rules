// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules

import io.nlopez.compose.core.ComposeKtConfig
import io.nlopez.compose.core.ComposeKtVisitor
import io.nlopez.compose.core.Emitter
import io.nlopez.compose.core.report
import io.nlopez.compose.core.util.composableLambdaTypes
import io.nlopez.compose.core.util.isLambda
import org.jetbrains.kotlin.psi.KtFunction

class ParameterNaming : ComposeKtVisitor {

    override fun visitComposable(
        function: KtFunction,
        autoCorrect: Boolean,
        emitter: Emitter,
        config: ComposeKtConfig,
    ) = with(config) {
        // For lambda parameters only: if it starts with `on`, we want it to not be in past tense, to be all consistent.
        // E.g. onClick, onTextChange, onValueChange, and a myriad of other examples in the compose foundation code.

        val lambdaTypes = function.containingKtFile.composableLambdaTypes

        val errors = function.valueParameters
            .filter { it.typeReference?.isLambda(lambdaTypes) == true }
            .filter {
                // As per why not force lambdas to all start with `on`, we cannot really know when they are used for
                // lazy initialization purposes -- and also don't want to be overly annoying.
                it.name?.startsWith("on") == true
            }
            .filter { it.name?.isPastTense == true }

        for (error in errors) {
            emitter.report(error, LambdaParametersInPresentTense)
        }
    }

    private val String.isPastTense: Boolean
        get() = endsWith("ed") || IrregularVerbsInPastTense.any { endsWith(it) }

    companion object {
        // A list of common irregular verbs in english, excluding those where present tense == past tense,
        // according to chatgpt. If you stumble upon one not here, feel free to send a PR to add it.
        private val IrregularVerbsInPastTense by lazy {
            setOf(
                "Arose",
                "Arisen",
                "Ate",
                "Awoke",
                "Awoken",
                "Beat",
                "Beaten",
                "Became",
                "Been",
                "Began",
                "Begun",
                "Bent",
                "Bit",
                "Bitten",
                "Bled",
                "Bled",
                "Blew",
                "Blown",
                "Bore",
                "Borne",
                "Bought",
                "Bound",
                "Bred",
                "Broke",
                "Broken",
                "Brought",
                "Built",
                "Burnt",
                "Burst",
                "Came",
                "Caught",
                "Chose",
                "Chosen",
                "Clung",
                "Cost",
                "Crept",
                "Dealt",
                "Did",
                "Done",
                "Drank",
                "Drawn",
                "Dreamt",
                "Drew",
                "Driven",
                "Drove",
                "Drunk",
                "Eaten",
                "Fallen",
                "Fed",
                "Felt",
                "Felt",
                "Fled",
                "Flew",
                "Flown",
                "Forbade",
                "Forbidden",
                "Forgave",
                "Forgiven",
                "Forgot",
                "Forgotten",
                "Fought",
                "Found",
                "Froze",
                "Frozen",
                "Gave",
                "Given",
                "Gone",
                "Got",
                "Gotten",
                "Grew",
                "Grown",
                "Had",
                "Heard",
                "Held",
                "Hid",
                "Hidden",
                "Hit",
                "Hung",
                "Hurt",
                "Kept",
                "Knew",
                "Known",
                "Laid",
                "Lain",
                "Lay",
                "Led",
                "Left",
                "Lent",
                "Let",
                "Lit",
                "Lost",
                "Made",
                "Meant",
                "Met",
                "Paid",
                "Ran",
                "Rang",
                "Ridden",
                "Risen",
                "Rode",
                "Rose",
                "Rung",
                "Said",
                "Sang",
                "Sank",
                "Sat",
                "Saw",
                "Seen",
                "Sent",
                "Set",
                "Shaken",
                "Shone",
                "Shook",
                "Shot",
                "Showed",
                "Shown",
                "Shrank",
                "Shrunk",
                "Slept",
                "Slid",
                "Sold",
                "Sought",
                "Spent",
                "Spoke",
                "Spoken",
                "Sprang",
                "Sprung",
                "Spun",
                "Stole",
                "Stolen",
                "Stood",
                "Struck",
                "Stuck",
                "Stung",
                "Sung",
                "Sunk",
                "Swam",
                "Swept",
                "Swore",
                "Sworn",
                "Swum",
                "Swung",
                "Taken",
                "Taught",
                "Thought",
                "Threw",
                "Thrown",
                "Told",
                "Took",
                "Tore",
                "Torn",
                "Understood",
                "Was",
                "Went",
                "Were",
                "Woke",
                "Woken",
                "Won",
                "Wore",
                "Worn",
                "Wound",
                "Written",
                "Wrote",
            )
        }

        val LambdaParametersInPresentTense = """
            Lambda parameters in a composable function should be in present tense, not past tense.

            Examples: `onClick` and not `onClicked`, `onTextChange` and not `onTextChanged`, etc.

            See https://mrmans0n.github.io/compose-rules/rules/#naming-parameters-properly for more information.
        """.trimIndent()
    }
}
