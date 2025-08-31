#!/usr/bin/env kotlin

@file:DependsOn("org.apache.velocity:velocity-engine-core:2.4.1")
@file:DependsOn("org.yaml:snakeyaml:2.5")

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringWriter
import java.util.Locale
import java.util.Properties
import kotlin.system.exitProcess

fun printUsage() {
    println("Usage: create-rule [RuleName]")
    println()
}

data class DetektRule(
    val id: String,
    val active: Boolean,
)

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

// Parse the config file
val yaml = Yaml()
val map: Map<String, Map<String, Map<String, Boolean>>> = detektConfig.bufferedReader().use { yaml.load(it) }
val currentDetektRules = map["Compose"]?.entries
    ?.map { (id, active) -> DetektRule(id = id, active = active["active"] == true) }.orEmpty()

// Check that all rules in the package match the active config
val newDetektRules = (currentDetektRules + listOf(DetektRule(id = ruleName, active = true)))
    .sortedBy { it.id }

// Create the new config with the rules ordered alphabetically by their ids
val newDetektConfig = buildString {
    appendLine("Compose:")
    for (rule in newDetektRules) {
        appendLine("  ${rule.id}:")
        appendLine("    active: ${rule.active}")
    }
}

detektConfig.writeText(newDetektConfig)

// Desirable improvements to add:
// - add rule to docs/detekt.md "default rule" values
// - add entry in docs/rules.md (likely at the end)
