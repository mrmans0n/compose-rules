// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.hexay.ktrs.KtrsOptions
import io.github.hexay.ktrs.spotless.KtrsStep
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.ktrs)
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    val libs = rootProject.libs

    pluginManager.apply(libs.plugins.spotless.get().pluginId)
    configure<SpotlessExtension> {
        // ktfmt 0.65 formatting via ktrs (native port), tuned to stay close to our previous ktlint intellij_idea style.
        val ktfmt = KtrsStep.create(KtrsOptions.kotlinlang().withMaxWidth(120))
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/samples/**/*.kt")
            addStep(ktfmt)

            licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
        }
        kotlinGradle {
            target("*.kts")
            targetExclude("**/samples/**/*.kts")
            addStep(ktfmt)
        }
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(24))
            }
            targetCompatibility = JavaVersion.VERSION_11
            sourceCompatibility = JavaVersion.VERSION_11
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            // Treat all Kotlin warnings as errors
            allWarningsAsErrors = true
            jvmTarget.set(JvmTarget.JVM_11)
            // Enable default methods in interfaces
            jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
        }
    }

    version = project.property("VERSION_NAME") ?: "0.0.0"
}

tasks.register("printVersion") {
    doLast {
        println(project.property("VERSION_NAME"))
    }
}
