// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
        val ktlintVersion = libs.versions.ktlint.get()
        kotlin {
            target("**/*.kt")
            ktlint(ktlintVersion)

            licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
        }
        kotlinGradle {
            target("*.kts")
            ktlint(ktlintVersion)
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
            freeCompilerArgs.addAll(
                // Enable default methods in interfaces
                "-Xjvm-default=all",
            )
        }
    }

    version = project.property("VERSION_NAME") ?: "0.0.0"
}

tasks.register("printVersion") {
    doLast {
        println(project.property("VERSION_NAME"))
    }
}
