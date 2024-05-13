// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.ktlint.rule.engine)
    implementation(projects.coreCommon)

    testImplementation(libs.ktlint.test)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
