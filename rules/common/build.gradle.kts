// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.kotlin.compiler)

    testImplementation(libs.junit5)
    testImplementation(libs.junit5.params)
    testImplementation(libs.assertj)
}
