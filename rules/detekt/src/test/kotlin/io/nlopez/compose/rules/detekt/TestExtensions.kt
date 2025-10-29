// Copyright 2025 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.detekt

import dev.detekt.api.Finding
import dev.detekt.api.SourceLocation
import dev.detekt.api.TextLocation
import org.assertj.core.api.AbstractListAssert
import org.assertj.core.api.Assertions
import org.assertj.core.api.ObjectAssert

/**
 * Wrapper for assertThat for lists of findings.
 */
fun assertThat(actual: List<Finding>): AbstractListAssert<*, List<Finding>, Finding, ObjectAssert<Finding>> =
    Assertions.assertThat(actual)

/**
 * Wrapper for assertThat for single finding.
 */
fun assertThat(actual: Finding): ObjectAssert<Finding> = Assertions.assertThat(actual)

/**
 * Asserts that the list of findings has the specified start source locations.
 */
fun AbstractListAssert<*, List<Finding>, Finding, ObjectAssert<Finding>>.hasStartSourceLocations(
    vararg locations: SourceLocation,
): AbstractListAssert<*, List<Finding>, Finding, ObjectAssert<Finding>> {
    extracting<SourceLocation> { finding: Finding -> finding.entity.location.source }
        .containsExactlyInAnyOrder(*locations)
    return this
}

/**
 * Asserts that the list of findings has the specified text locations.
 * In detekt 2.0, we extract the name from the ktElement.
 */
fun AbstractListAssert<*, List<Finding>, Finding, ObjectAssert<Finding>>.hasTextLocations(
    vararg locations: String,
): AbstractListAssert<*, List<Finding>, Finding, ObjectAssert<Finding>> {
    extracting<String> { finding: Finding ->
        // Extract the name from named declarations
        (finding.entity.ktElement as? org.jetbrains.kotlin.psi.KtNamedDeclaration)?.name
            ?: finding.entity.signature
    }.containsExactlyInAnyOrder(*locations)
    return this
}

/**
 * Asserts that a single finding has the specified start source location.
 */
fun ObjectAssert<Finding>.hasStartSourceLocation(location: SourceLocation): ObjectAssert<Finding> {
    extracting { it.entity.location.source }.isEqualTo(location)
    return this
}

/**
 * Asserts that a single finding has the specified start source location (by line and column).
 */
fun ObjectAssert<Finding>.hasStartSourceLocation(line: Int, column: Int): ObjectAssert<Finding> {
    extracting { it.entity.location.source }.isEqualTo(SourceLocation(line, column))
    return this
}

/**
 * Asserts that the list contains exactly one finding with the specified start source location.
 */
fun AbstractListAssert<*, List<Finding>, Finding, ObjectAssert<Finding>>.hasStartSourceLocation(
    line: Int,
    column: Int,
): AbstractListAssert<*, List<Finding>, Finding, ObjectAssert<Finding>> {
    hasSize(1)
    first().hasStartSourceLocation(line, column)
    return this
}

/**
 * Asserts that the finding has the specified message.
 */
fun ObjectAssert<Finding>.hasMessage(expectedMessage: String): ObjectAssert<Finding> {
    extracting { it.message }.isEqualTo(expectedMessage)
    return this
}

/**
 * Extension to get location from a Finding for easier assertions.
 */
val Finding.startLocation: SourceLocation
    get() = entity.location.source
