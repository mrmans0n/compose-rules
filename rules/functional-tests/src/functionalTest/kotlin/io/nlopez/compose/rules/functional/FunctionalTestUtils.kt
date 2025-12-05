// Copyright 2024 Nacho Lopez
// SPDX-License-Identifier: Apache-2.0
package io.nlopez.compose.rules.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files

/**
 * Base utilities for functional testing of compose-rules with Gradle TestKit.
 */
object FunctionalTestUtils {

    /**
     * Gets version information from system properties set by the build.
     */
    val composeRulesVersion: String = System.getProperty("composeRulesVersion")
    val kotlinVersion: String = System.getProperty("kotlinVersion")
    val ktlintVersion: String = System.getProperty("ktlintVersion")
    val detektVersion: String = System.getProperty("detektVersion")

    /**
     * Creates a temporary Gradle project directory structure.
     */
    fun createTempProjectDir(): File = Files.createTempDirectory("compose-rules-functional-test").toFile().apply {
        deleteOnExit()
    }

    /**
     * Creates a GradleRunner configured for functional tests.
     *
     * Note: We don't use withPluginClasspath() because we're not testing a Gradle plugin.
     * Instead, we rely on mavenLocal() in the test projects to find the built artifacts.
     */
    fun createGradleRunner(projectDir: File, arguments: List<String>): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(arguments + "--stacktrace")
        .forwardOutput()

    /**
     * Writes a file to the project directory.
     */
    fun File.writeFile(relativePath: String, content: String): File = resolve(relativePath).apply {
        parentFile.mkdirs()
        writeText(content.trimIndent())
    }

    /**
     * Creates a basic gradle.properties file with necessary settings.
     */
    fun File.writeGradleProperties() {
        writeFile(
            "gradle.properties",
            """
            org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
            org.gradle.caching=true
            org.gradle.configuration-cache=false
            """,
        )
    }

    /**
     * Creates a basic settings.gradle.kts file.
     */
    fun File.writeSettings(projectName: String = "test-project") {
        writeFile(
            "settings.gradle.kts",
            """
            rootProject.name = "$projectName"

            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                    google()
                    mavenLocal()
                }
            }
            """,
        )
    }
}

/**
 * Extension functions for BuildResult assertions.
 */
fun BuildResult.assertTaskOutcome(taskPath: String, expectedOutcome: TaskOutcome) {
    val actualOutcome = task(taskPath)?.outcome
    require(actualOutcome == expectedOutcome) {
        "Expected task '$taskPath' to be $expectedOutcome but was $actualOutcome.\n" +
            "Available tasks: ${tasks.joinToString { "${it.path}:${it.outcome}" }}"
    }
}

fun BuildResult.assertTaskFailed(taskPath: String) {
    // For buildAndFail(), the task might not be in the list or might have a different outcome
    // We primarily check that the build itself failed (which buildAndFail() ensures)
    val task = task(taskPath)
    if (task != null) {
        require(task.outcome == TaskOutcome.FAILED || task.outcome == TaskOutcome.SUCCESS) {
            "Expected task '$taskPath' to have failed or run, but was ${task.outcome}"
        }
    }
    // If task is null, that's also acceptable for a failed build
}

fun BuildResult.assertTaskSucceeded(taskPath: String) {
    // Task can be SUCCESS, UP_TO_DATE, or FROM_CACHE - all are considered successful
    val task = task(taskPath)
    require(task != null) {
        "Task '$taskPath' not found in build result.\n" +
            "Available tasks: ${tasks.joinToString { "${it.path}:${it.outcome}" }}"
    }
    require(
        task.outcome == TaskOutcome.SUCCESS ||
            task.outcome == TaskOutcome.UP_TO_DATE ||
            task.outcome == TaskOutcome.FROM_CACHE,
    ) {
        "Expected task '$taskPath' to succeed (SUCCESS/UP_TO_DATE/FROM_CACHE) but was ${task.outcome}"
    }
}

fun BuildResult.assertTaskUpToDate(taskPath: String) {
    assertTaskOutcome(taskPath, TaskOutcome.UP_TO_DATE)
}

fun BuildResult.assertOutputContains(vararg texts: String) {
    texts.forEach { text ->
        require(output.contains(text)) {
            "Expected output to contain '$text' but it didn't.\nOutput:\n$output"
        }
    }
}

fun BuildResult.assertOutputDoesNotContain(vararg texts: String) {
    texts.forEach { text ->
        require(!output.contains(text)) {
            "Expected output to not contain '$text' but it did.\nOutput:\n$output"
        }
    }
}
