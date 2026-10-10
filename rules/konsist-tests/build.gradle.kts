// Copyright 2026 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Konsist parses sources, so this module intentionally doesn't depend on the rule modules: that keeps its
// Kotlin compiler away from the ktlint/detekt ones. Not published, so it can use the JVM target Konsist requires.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.test {
    // Konsist reads the sources of other modules, so they must be inputs for the task to rerun when they change
    inputs.files(rootProject.fileTree("rules") { include("*/src/**/*.kt") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit5)
    testImplementation(libs.junit5.params)
    testImplementation(libs.junit5.engine)
    testImplementation(libs.junit5.platform.launcher)
}
