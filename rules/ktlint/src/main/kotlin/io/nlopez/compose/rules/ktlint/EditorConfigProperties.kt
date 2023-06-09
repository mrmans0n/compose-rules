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
            PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
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
            PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
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
            PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
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
            PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
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
