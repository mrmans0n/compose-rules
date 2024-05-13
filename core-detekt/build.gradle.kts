// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.detekt.core)
    implementation(projects.coreCommon)

    testImplementation(libs.detekt.test)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
