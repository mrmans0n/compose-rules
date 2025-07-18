// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.mavenPublish)
}

dependencies {
    compileOnlyOrApi(libs.kotlin.compiler.ktlint)

    testImplementation(libs.junit5)
    testImplementation(libs.junit5.params)
    testImplementation(libs.assertj)
}

// Include the source code from the main common module
sourceSets {
    main {
        kotlin.srcDirs("../common/src/main/kotlin")
    }
    test {
        kotlin.srcDirs("../common/src/test/kotlin")
    }
}
