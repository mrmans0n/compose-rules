// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
}

// Get compose-rules version from root project's gradle.properties
val composeRulesVersion: String by lazy {
    val props = Properties()
    file("../../gradle.properties").inputStream().use { props.load(it) }
    props.getProperty("VERSION_NAME")
}

repositories {
    mavenCentral()
    google()
}

val ktlintCli = configurations.create("ktlintCli") {
    isCanBeConsumed = false
}
val composeRules = configurations.create("composeRules") {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    add(ktlintCli.name, libs.ktlint.runner)
    add(composeRules.name, "io.nlopez.compose.rules:ktlint:$composeRulesVersion")
}

tasks.register<JavaExec>("ktlintCheck") {
    classpath = ktlintCli
    mainClass.set("io.github.ktlint.core.Main")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt()))
    })
    argumentProviders.add(CommandLineArgumentProvider {
        listOf("--ruleset=${composeRules.singleFile.absolutePath}", "src/**/*.kt")
    })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}
