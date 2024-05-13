// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    val libs = rootProject.libs

    pluginManager.apply(libs.plugins.spotless.get().pluginId)
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            // ktlint("1.1.1")
            ktlint(libs.versions.ktlint.get())

            licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
        }
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            // Treat all Kotlin warnings as errors
            allWarningsAsErrors = true
            freeCompilerArgs.addAll(
                // Enable default methods in interfaces
                "-Xjvm-default=all",
                // Enable context receivers
                "-Xcontext-receivers",
                // Enable K2 compiler
                "-language-version=2.0",
                "-Xsuppress-version-warnings",
            )
        }
    }

    version = project.property("VERSION_NAME") ?: "0.0.0"
    if (project != rootProject) {
        pluginManager.apply(libs.plugins.mavenPublish.get().pluginId)
    }
}

tasks.register("printVersion") {
    doLast {
        println(project.property("VERSION_NAME"))
    }
}
