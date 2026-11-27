// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadowJar)
    alias(libs.plugins.mavenPublish)
}

if (!project.hasProperty("uberJar")) {
    afterEvaluate {
        val javaComponent = components["java"] as AdhocComponentWithVariants
        javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
            skip()
        }
    }
}

tasks.shadowJar {
    // Shared Compose checks are already compiled into this module. The CLI supplies ktlint and its PSI classes.
    configurations.set(emptySet())
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    api(libs.ktlint.cli.ruleset.core)
    // The alpha's PSI API references IntelliJ classes such as NlsSafe that are
    // only present in the implementation artifact. ktlint supplies it at runtime.
    compileOnly(libs.kotlin.ktlint.intellij) {
        isTransitive = false
    }

    testImplementation(libs.ktlint.rule.engine)
    testImplementation(libs.ktlint.ruleset.standard)
    testImplementation(libs.ktlint.test)
    testRuntimeOnly(libs.junit5.platform.launcher)
    testImplementation(libs.reflections)
}

sourceSets.main {
    kotlin.srcDir("../common/src/main/kotlin")
}
