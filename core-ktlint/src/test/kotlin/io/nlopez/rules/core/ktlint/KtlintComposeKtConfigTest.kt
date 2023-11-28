// Copyright 2023 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.rules.core.ktlint

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.LowerCasingPropertyType
import org.ec4j.core.model.PropertyType.PropertyValue.valid
import org.ec4j.core.model.PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER
import org.junit.jupiter.api.Test

class KtlintComposeKtConfigTest {
    private val mapping = mutableMapOf<String, Property>().apply {
        put("compose_my_int", "10".prop)
        put("compose_my_string", "abcd".prop)
        put("compose_my_list", "a,b,c,a".prop)
        put("compose_my_list2", "a , b , c,a".prop)
        put("compose_my_set", "a,b,c,a,b,c".prop)
        put("compose_my_set2", "  a, b,c ,a  , b  ,  c ".prop)
        put("compose_my_bool", true.prop)
        put("compose_this_is_broken", "asdf".prop)
    }

    private val properties: EditorConfig = EditorConfig(mapping)
    private val config = KtlintComposeKtConfig(
        properties = properties,
        editorConfigProperties = setOf(
            stringProperty("compose_my_int"),
            stringProperty("compose_my_string"),
            stringProperty("compose_my_list"),
            stringProperty("compose_my_list2"),
            stringProperty("compose_my_set"),
            stringProperty("compose_my_set2"),
            booleanProperty("compose_my_bool"),
        ),
    )

    @Test
    fun `returns ints from properties`() {
        assertThat(config.getInt("myInt", 0)).isEqualTo(10)
    }

    @Test
    fun `returns strings from properties`() {
        assertThat(config.getString("myString", null)).isEqualTo("abcd")
    }

    @Test
    fun `returns lists from properties`() {
        assertThat(config.getList("myList", emptyList())).containsExactly("a", "b", "c", "a")
        assertThat(config.getList("myList2", emptyList())).containsExactly("a", "b", "c", "a")
    }

    @Test
    fun `returns sets from properties`() {
        assertThat(config.getSet("mySet", emptySet())).containsExactly("a", "b", "c")
        assertThat(config.getSet("mySet2", emptySet())).containsExactly("a", "b", "c")
    }

    @Test
    fun `returns boolean from properties`() {
        assertThat(config.getBoolean("myBool", false)).isTrue()
    }

    @Test
    fun `results are memoized`() {
        assertThat(config.getInt("myInt", 0)).isEqualTo(10)
        assertThat(config.getString("myString", null)).isEqualTo("abcd")
        assertThat(config.getList("myList", emptyList())).containsExactly("a", "b", "c", "a")
        assertThat(config.getList("myList2", emptyList())).containsExactly("a", "b", "c", "a")
        assertThat(config.getSet("mySet", emptySet())).containsExactly("a", "b", "c")
        assertThat(config.getSet("mySet2", emptySet())).containsExactly("a", "b", "c")
        assertThat(config.getBoolean("myBool", false)).isTrue()

        mapping["my_int"] = "100".prop
        mapping["my_string"] = "XYZ".prop
        mapping["my_list"] = "z,y,x".prop
        mapping["my_list2"] = "z,y".prop
        mapping["my_set"] = "a".prop
        mapping["my_set2"] = "a, b".prop
        mapping["my_bool"] = false.prop

        assertThat(config.getInt("myInt", 0)).isEqualTo(10)
        assertThat(config.getString("myString", null)).isEqualTo("abcd")
        assertThat(config.getList("myList", emptyList())).containsExactly("a", "b", "c", "a")
        assertThat(config.getList("myList2", emptyList())).containsExactly("a", "b", "c", "a")
        assertThat(config.getSet("mySet", emptySet())).containsExactly("a", "b", "c")
        assertThat(config.getSet("mySet2", emptySet())).containsExactly("a", "b", "c")
        assertThat(config.getBoolean("myBool", false)).isTrue()
    }

    @Test
    fun `missing predefined property is returned as default`() {
        assertThat(config.getString("this_is_broken", "")).isEqualTo("")
    }

    private val String.prop: Property
        get() = Property.builder().value(this).build()

    private val Boolean.prop: Property
        get() = Property.builder()
            .type(LowerCasingPropertyType("", "", BOOLEAN_VALUE_PARSER, "true", "false"))
            .value(
                when (this) {
                    true -> valid("true", true)
                    false -> valid("false", false)
                },
            )
            .build()

    private fun booleanProperty(key: String, default: Boolean = false): EditorConfigProperty<Boolean> =
        EditorConfigProperty(
            type = LowerCasingPropertyType(
                key,
                "Internal boolean value for $key",
                BOOLEAN_VALUE_PARSER,
                "true",
                "false",
            ),
            defaultValue = default,
        )

    private fun stringProperty(key: String, default: String = ""): EditorConfigProperty<String> = EditorConfigProperty(
        type = LowerCasingPropertyType(
            key,
            "Internal string value for $key",
            PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
            emptySet(),
        ),
        defaultValue = default,
        propertyMapper = { property, _ ->
            when {
                property?.isUnset == true -> ""
                property?.getValueAs<String>() != null -> property.getValueAs<String>()
                else -> property?.getValueAs()
            }
        },
    )
}
