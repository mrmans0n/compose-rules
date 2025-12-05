// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Define a custom source set for functional tests
sourceSets {
    val functionalTest by creating {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val functionalTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val functionalTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    functionalTestImplementation(gradleTestKit())
    functionalTestImplementation(libs.junit5)
    functionalTestImplementation(libs.junit5.params)
    functionalTestImplementation(libs.assertj)

    functionalTestRuntimeOnly(libs.junit5.engine)
    functionalTestRuntimeOnly(libs.junit5.platform.launcher)

    // Include the actual rule projects to access their JARs
    functionalTestImplementation(projects.rules.detekt)
    functionalTestImplementation(projects.rules.ktlint)
}

// Read versions from files
fun readVersionFromToml(propertyName: String): String {
    val tomlFile = rootProject.file("gradle/libs.versions.toml")
    val versionLine = tomlFile.readLines().find { it.trim().startsWith("$propertyName =") }
        ?: error("Version $propertyName not found in libs.versions.toml")
    return versionLine.substringAfter("\"").substringBefore("\"")
}

val composeRulesVersionValue = project.version.toString()
val kotlinVersionValue = readVersionFromToml("kotlin")
val ktlintVersionValue = readVersionFromToml("ktlint")
val detektVersionValue = readVersionFromToml("detekt")

// Register the functional test task
val functionalTest by tasks.registering(Test::class) {
    description = "Runs functional tests."
    group = "verification"

    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath

    useJUnitPlatform()

    // Pass version information to tests via system properties
    systemProperty("composeRulesVersion", composeRulesVersionValue)
    systemProperty("kotlinVersion", kotlinVersionValue)
    systemProperty("ktlintVersion", ktlintVersionValue)
    systemProperty("detektVersion", detektVersionValue)

    // Make built artifacts available to tests
    dependsOn(":rules:detekt:assemble", ":rules:ktlint:assemble")

    // Set test output for better debugging
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.check {
    dependsOn(functionalTest)
}
