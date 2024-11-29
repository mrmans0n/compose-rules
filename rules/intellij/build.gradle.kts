plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        instrumentationTools()
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
    }

    implementation(projects.rules.common)
}

intellijPlatform {
    pluginConfiguration {
        id = "io.nlopez.compose.rules.intellij"
        name = "Compose Rules"
        version = project.property("VERSION_NAME").toString()
        description = "Compose Rules IntelliJ Plugin"
    }
}
