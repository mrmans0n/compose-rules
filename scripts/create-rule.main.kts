#!/usr/bin/env kotlin

@file:DependsOn("org.apache.velocity:velocity-engine-core:2.4.1")

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import java.io.File
import java.io.StringWriter
import java.util.Locale
import java.util.Properties
import kotlin.system.exitProcess

fun printUsage() {
    println("Usage: create-rule [RuleName]")
    println()
}

private val humps by lazy { "(?<=.)(?=\\p{Upper})".toRegex() }

fun String.toKebabCase() = replace(humps, "-").lowercase(Locale.getDefault())

fun VelocityEngine.writeTemplate(
    templateName: String,
    targetDirectory: File,
    targetName: String,
    context: VelocityContext
) {
    val targetFile = targetDirectory.resolve("${targetName}.kt")
    if (targetFile.exists()) {
        println("Can't write $templateName to $targetFile, file already exists. Delete and run again.")
        return
    }
    println("--> Writing to $targetFile...")
    val template = getTemplate(templateName)
    val writer = StringWriter()
    template.merge(context, writer)
    targetFile.writeText(writer.toString())
}

// main code

if (args.isEmpty()) {
    printUsage()
    exitProcess(1)
}
val newRule = args.singleOrNull()
when {
    newRule == null -> {
        println("Only 1 parameter supported.")
        printUsage()
        exitProcess(2)
    }

    newRule.endsWith("Rule") || newRule.endsWith("Check") -> {
        println("Do not add 'Rule' or 'Check' suffix, it will result in weird and repetitive naming.")
        printUsage()
        exitProcess(2)
    }
}

val ruleName = requireNotNull(newRule)

println("Finding project root...")
var rootDir = File(System.getProperty("user.dir"))
while (!rootDir.resolve("settings.gradle.kts").exists()) {
    rootDir = rootDir.parentFile
}

println("Setting up templates...")

val engine = VelocityEngine(
    Properties().apply {
        setProperty(
            RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
            "templates"
        ) // Adjust the path to your templates directory
    }
).apply { init() }

val context = VelocityContext().apply {
    put("ruleName", ruleName)
    put("detektRuleName", "${newRule}Check")
    put("ktlintRuleName", "${newRule}Check")
    put("ktlintRuleId", ruleName.toKebabCase())
}

println("Applying templates...")

// Write main rule
engine.writeTemplate(
    templateName = "Rule.kt.template",
    targetDirectory = rootDir.resolve("rules/common/src/main/kotlin/io/nlopez/compose/rules/"),
    targetName = ruleName,
    context = context
)

// Write detekt rule that delegates to main rule
engine.writeTemplate(
    templateName = "DetektRule.kt.template",
    targetDirectory = rootDir.resolve("rules/detekt/src/main/kotlin/io/nlopez/compose/rules/detekt/"),
    targetName = "${ruleName}Check",
    context = context
)

// Write test for detekt rule
engine.writeTemplate(
    templateName = "DetektRuleTest.kt.template",
    targetDirectory = rootDir.resolve("rules/detekt/src/test/kotlin/io/nlopez/compose/rules/detekt/"),
    targetName = "${ruleName}CheckTest",
    context = context
)

// Write ktlint rule that delegates to main rule
engine.writeTemplate(
    templateName = "KtlintRule.kt.template",
    targetDirectory = rootDir.resolve("rules/ktlint/src/main/kotlin/io/nlopez/compose/rules/ktlint/"),
    targetName = "${ruleName}Check",
    context = context
)

// Write test for ktlint rule
engine.writeTemplate(
    templateName = "KtlintRuleTest.kt.template",
    targetDirectory = rootDir.resolve("rules/ktlint/src/test/kotlin/io/nlopez/compose/rules/ktlint/"),
    targetName = "${ruleName}CheckTest",
    context = context
)

println("Adding to detekt default ruleset...")
val detektConfig = rootDir.resolve("rules/detekt/src/main/resources/config/config.yml")
detektConfig.appendText("""
  $ruleName:
    active: true
"""
)
// Desirable improvements to add:
// - add rule to docs/detekt.md "default rule" values
// - add entry in docs/rules.md (likely at the end)
