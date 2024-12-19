// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadowJar)
}

// if publishing and it's not the uber jar, we want to remove the shadowRuntimeElements variant
if (!project.hasProperty("uberJar")) {
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
        skip()
    }
}

tasks.shadowJar {
    // Relocate packages that may conflict with the ones IntelliJ IDEA provides as well.
    // See https://github.com/nbadal/ktlint-intellij-plugin/blob/main/lib/build.gradle.kts
    relocate("org.jetbrains.concurrency", "shadow.org.jetbrains.concurrency")
    relocate("org.jetbrains.kotlin.psi.KtPsiFactory", "shadow.org.jetbrains.kotlin.psi.KtPsiFactory")
    relocate("org.jetbrains.kotlin.psi.psiUtil", "shadow.org.jetbrains.kotlin.psi.psiUtil")
    relocate("org.jetbrains.org", "shadow.org.jetbrains.org")
}

dependencies {
    compileOnlyOrApi(libs.ktlint.rule.engine)
    compileOnlyOrApi(libs.ktlint.cli.ruleset.core)
    api(projects.rules.common)

    testImplementation(libs.ktlint.test)
    testImplementation(libs.junit5)
    testImplementation(libs.junit5.params)
    testImplementation(libs.assertj)
    testImplementation(libs.reflections)
    testImplementation(libs.konsist)
}
