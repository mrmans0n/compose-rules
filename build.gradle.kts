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
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            // Treat all Kotlin warnings as errors
            allWarningsAsErrors = true
            freeCompilerArgs.addAll(
                // Enable default methods in interfaces
                "-Xjvm-default=all",
            )
        }
    }

    version = project.property("VERSION_NAME") ?: "0.0.0"
    if (project != rootProject) {
        pluginManager.apply(libs.plugins.mavenPublish.get().pluginId)
    }

    project.configurations.create("compileOnlyOrApi") {
        isCanBeConsumed = false
        isCanBeResolved = true

        project.configurations.configureEach {
            when {
                name == "api" && project.hasProperty("uberJar") -> extendsFrom(this@create)
                name == "compileOnly" -> extendsFrom(this@create)
                name == "testImplementation" -> extendsFrom(this@create)
            }
        }
    }
}

tasks.register("printVersion") {
    doLast {
        println(project.property("VERSION_NAME"))
    }
}
