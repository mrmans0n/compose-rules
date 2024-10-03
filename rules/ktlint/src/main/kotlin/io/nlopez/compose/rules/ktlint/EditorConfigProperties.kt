// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.ktlint

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser

val contentEmittersProperty: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_content_emitters",
            "A comma separated list of composable functions that emit content (e.g. UI)",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val contentEmittersDenylist: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_content_emitters_denylist",
            "A comma separated list of composable functions that we don't want to take into acccount " +
                "when assessing if something is a content emitter",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val checkModifiersForVisibility: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_check_modifiers_for_visibility",
            "Visibility of the composables where we want to check if a Modifier parameter is missing",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            setOf("only_public", "public_and_internal", "all"),
        ),
        defaultValue = "only_public",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> "only_public"
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val compositionLocalAllowlistProperty: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_allowed_composition_locals",
            "A comma separated list of allowed CompositionLocals",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val allowedComposeNamingNames: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_allowed_composable_function_names",
            "A comma separated list of regexes of allowed composable function names",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val viewModelFactories: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_view_model_factories",
            "A comma separated list of ViewModel factory methods",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val allowedStateHolderNames: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_allowed_state_holder_names",
            "A comma separated list of regexes of valid state holders / ViewModel / Presenter names",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val allowedForwarding: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_allowed_forwarding",
            "A comma separated list of regexes of composable names where forwarding a " +
                "state holder / ViewModel / Presenter names is alright to do",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val allowedForwardingOfTypes: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_allowed_forwarding_of_types",
            "A comma separated list of regexes of state holder/ViewModel names which are exempt from " +
                "the forwarding rule",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val customModifiers: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_custom_modifiers",
            "A comma separated list of custom Modifier implementations",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val treatAsLambda: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_treat_as_lambda",
            "A comma separated list of types that should be treated as lambdas " +
                "(e.g. typedefs of lambdas, fun interfaces)",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val treatAsComposableLambda: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_treat_as_composable_lambda",
            "A comma separated list of types that should be treated as @Composable lambdas " +
                "(e.g. typedefs of lambdas, fun interfaces)",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val allowedFromM2: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_allowed_from_m2",
            "A comma separated list of Material 2 APIs that are allowed",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val disallowMaterial2: EditorConfigProperty<Boolean> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_disallow_material2",
            "When enabled, Compose Material 2 (M2) usages will be disallowed.",
            PropertyValueParser.BOOLEAN_VALUE_PARSER,
            setOf(true.toString(), false.toString()),
        ),
        defaultValue = false,
    )

val disallowUnstableCollections: EditorConfigProperty<Boolean> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_disallow_unstable_collections",
            "When enabled, unstable collections (e.g. List/Set/Map) will be disallowed.",
            PropertyValueParser.BOOLEAN_VALUE_PARSER,
            setOf(true.toString(), false.toString()),
        ),
        defaultValue = false,
    )

val modifierMissingIgnoreAnnotated: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_modifier_missing_ignore_annotated",
            "A comma separated list of composable functions that should be exempt from the ModifierMissing check",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )

val composePreviewNamingEnabled: EditorConfigProperty<Boolean> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_preview_naming_enabled",
            "When enabled, preview composables should follow the configured naming strategy.",
            PropertyValueParser.BOOLEAN_VALUE_PARSER,
            setOf(true.toString(), false.toString()),
        ),
        defaultValue = false,
    )

val composePreviewNamingStrategy: EditorConfigProperty<String> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "compose_preview_naming_strategy",
            "The naming strategy for preview composables.",
            PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = "suffix",
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )
