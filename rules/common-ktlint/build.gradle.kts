// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.mavenPublish)
}

dependencies {
    api(libs.kotlin.compiler.ktlint)

    testImplementation(libs.junit5)
    testImplementation(libs.junit5.params)
    testImplementation(libs.assertj)
}

val generateCommonKtlintSources by tasks.registering(Copy::class) {
    from("../common/src/main/kotlin")
    into(layout.buildDirectory.dir("generated/sources/commonKtlint/kotlin"))
    filter { line ->
        line.replace("com.intellij", "org.jetbrains.kotlin.com.intellij")
    }
}

val generateCommonKtlintTestSources by tasks.registering(Copy::class) {
    from("../common/src/test/kotlin")
    into(layout.buildDirectory.dir("generated/sources/commonKtlint/test"))
    filter { line ->
        line.replace("com.intellij", "org.jetbrains.kotlin.com.intellij")
    }
}

// Include the source code from the main common module
sourceSets {
    main {
        kotlin.srcDir(generateCommonKtlintSources.map { it.outputs.files.singleFile })
    }
    test {
        kotlin.srcDir(generateCommonKtlintTestSources.map { it.outputs.files.singleFile })
    }
}
