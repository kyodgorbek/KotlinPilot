package io.kotlinpilot.infrastructure.analyzer

import io.kotlinpilot.core.domain.ArchitectureHint
import io.kotlinpilot.core.domain.ModuleInfo
import io.kotlinpilot.core.domain.ProjectAnalysis
import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import java.io.File

class RepositoryAnalyzer(
    private val securityPolicy: WorkspaceSecurityPolicy
) {
    fun analyze(): ProjectAnalysis {
        val root = securityPolicy.getWorkspaceRoot().toFile()
        val projectName = root.name

        val settingsKts = File(root, "settings.gradle.kts")
        val settingsGroovy = File(root, "settings.gradle")
        val rootBuildKts = File(root, "build.gradle.kts")
        val rootBuildGroovy = File(root, "build.gradle")
        val isGradle = settingsKts.exists() || settingsGroovy.exists() || rootBuildKts.exists() || rootBuildGroovy.exists()

        val isKotlin = hasKotlinFiles(root) || rootBuildKts.exists() || settingsKts.exists()

        // Discover modules
        val modules = discoverModules(root, settingsKts, settingsGroovy)

        // Read all build scripts for dependency / plugin analysis
        val allBuildFiles = mutableListOf<File>()
        if (rootBuildKts.exists()) allBuildFiles.add(rootBuildKts)
        if (rootBuildGroovy.exists()) allBuildFiles.add(rootBuildGroovy)
        modules.forEach { mod ->
            val modBuildKts = File(root, "${mod.path}/build.gradle.kts")
            val modBuildGroovy = File(root, "${mod.path}/build.gradle")
            if (modBuildKts.exists()) allBuildFiles.add(modBuildKts)
            if (modBuildGroovy.exists()) allBuildFiles.add(modBuildGroovy)
        }

        val versionCatalog = File(root, "gradle/libs.versions.toml")
        if (versionCatalog.exists()) {
            allBuildFiles.add(versionCatalog)
        }

        val combinedBuildContent = allBuildFiles.joinToString("\n") { it.readText() }

        // Versions
        val kotlinVersion = extractKotlinVersion(combinedBuildContent)
        val gradleVersion = extractGradleVersion(root)
        val agpVersion = extractAgpVersion(combinedBuildContent)

        // Android / Compose / KMP detection
        val isCompose = combinedBuildContent.contains("compose", ignoreCase = true) || combinedBuildContent.contains("androidx.compose")
        val isKmp = combinedBuildContent.contains("kotlin(\"multiplatform\")") || combinedBuildContent.contains("org.jetbrains.kotlin.multiplatform")

        // Architecture hints
        val architectureHints = detectArchitectureHints(combinedBuildContent, root)

        // Test frameworks
        val testFrameworks = detectTestFrameworks(combinedBuildContent)

        // Custom rules
        val rulesFile = File(root, ".kotlinpilot/rules.md")
        val customRules = if (rulesFile.exists()) rulesFile.readText() else null

        // Important files
        val importantFiles = discoverImportantFiles(root, modules)

        return ProjectAnalysis(
            projectName = projectName,
            rootPath = root.absolutePath,
            isKotlin = isKotlin,
            isGradle = isGradle,
            kotlinVersion = kotlinVersion,
            gradleVersion = gradleVersion,
            agpVersion = agpVersion,
            composeEnabled = isCompose,
            kmpEnabled = isKmp,
            modules = modules,
            architectureHints = architectureHints,
            testFrameworks = testFrameworks,
            customRules = customRules,
            importantFiles = importantFiles
        )
    }

    private fun hasKotlinFiles(dir: File): Boolean {
        return dir.walkTopDown().maxDepth(5).any { it.isFile && (it.extension == "kt" || it.extension == "kts") }
    }

    private fun discoverModules(root: File, settingsKts: File, settingsGroovy: File): List<ModuleInfo> {
        val modules = mutableListOf<ModuleInfo>()

        val settingsContent = when {
            settingsKts.exists() -> settingsKts.readText()
            settingsGroovy.exists() -> settingsGroovy.readText()
            else -> ""
        }

        val includeRegex = Regex("include\\s*\\(?\\s*['\"]([^'\"]+)['\"]\\s*\\)?")
        val matches = includeRegex.findAll(settingsContent)

        for (match in matches) {
            val rawName = match.groupValues[1].removePrefix(":")
            val path = rawName.replace(':', '/')
            val modDir = File(root, path)
            if (modDir.exists() && modDir.isDirectory) {
                val modBuild = listOf(File(modDir, "build.gradle.kts"), File(modDir, "build.gradle"))
                    .firstOrNull { it.exists() }?.readText() ?: ""
                
                val isAndroid = modBuild.contains("com.android.") || File(modDir, "src/main/AndroidManifest.xml").exists()
                val isCompose = modBuild.contains("compose", ignoreCase = true)
                val isKmp = modBuild.contains("multiplatform")

                modules.add(
                    ModuleInfo(
                        name = rawName,
                        path = path,
                        isAndroid = isAndroid,
                        isCompose = isCompose,
                        isKmp = isKmp
                    )
                )
            }
        }

        if (modules.isEmpty()) {
            // Root project itself is the single module
            val rootBuild = listOf(File(root, "build.gradle.kts"), File(root, "build.gradle"))
                .firstOrNull { it.exists() }?.readText() ?: ""
            modules.add(
                ModuleInfo(
                    name = "root",
                    path = ".",
                    isAndroid = rootBuild.contains("com.android."),
                    isCompose = rootBuild.contains("compose", ignoreCase = true),
                    isKmp = rootBuild.contains("multiplatform")
                )
            )
        }

        return modules
    }

    private fun extractKotlinVersion(buildContent: String): String? {
        val regex = Regex("kotlin[\\w.]*\\s*(?:version)?\\s*['\"]([0-9]+\\.[0-9]+(\\.[0-9]+)?)['\"]")
        val match = regex.find(buildContent)
        return match?.groupValues?.get(1)
    }

    private fun extractGradleVersion(root: File): String? {
        val wrapperProps = File(root, "gradle/wrapper/gradle-wrapper.properties")
        if (wrapperProps.exists()) {
            val content = wrapperProps.readText()
            val regex = Regex("gradle-([0-9]+\\.[0-9]+(\\.[0-9]+)?)-")
            return regex.find(content)?.groupValues?.get(1)
        }
        return null
    }

    private fun extractAgpVersion(buildContent: String): String? {
        val regex = Regex("com\\.android\\.(?:application|library)\\s*['\"]?version['\"]?\\s*['\"]([0-9]+\\.[0-9]+(\\.[0-9]+)?)['\"]")
        return regex.find(buildContent)?.groupValues?.get(1)
    }

    private fun detectArchitectureHints(buildContent: String, root: File): List<ArchitectureHint> {
        val hints = mutableListOf<ArchitectureHint>()

        // Dependency Injection
        if (buildContent.contains("io.insert-koin") || buildContent.contains("koin")) {
            hints.add(ArchitectureHint("Dependency Injection", "Koin", 1.0, "Found Koin dependencies in build scripts"))
        } else if (buildContent.contains("com.google.dagger:hilt") || buildContent.contains("dagger.hilt")) {
            hints.add(ArchitectureHint("Dependency Injection", "Hilt/Dagger", 1.0, "Found Hilt/Dagger dependencies in build scripts"))
        }

        // UI
        if (buildContent.contains("androidx.compose") || buildContent.contains("jetbrains.compose")) {
            hints.add(ArchitectureHint("UI Framework", "Jetpack Compose", 1.0, "Compose compiler/runtime detected"))
        }

        // Networking
        if (buildContent.contains("io.ktor:ktor-client")) {
            hints.add(ArchitectureHint("Networking", "Ktor Client", 1.0, "Found Ktor client dependencies"))
        } else if (buildContent.contains("com.squareup.retrofit2")) {
            hints.add(ArchitectureHint("Networking", "Retrofit", 1.0, "Found Retrofit dependencies"))
        }

        // Persistence
        if (buildContent.contains("androidx.room")) {
            hints.add(ArchitectureHint("Persistence", "Room Database", 1.0, "Found AndroidX Room dependencies"))
        } else if (buildContent.contains("app.cash.sqldelight")) {
            hints.add(ArchitectureHint("Persistence", "SQLDelight", 1.0, "Found SQLDelight dependencies"))
        }

        // Concurrency / Reactive
        if (buildContent.contains("kotlinx-coroutines")) {
            hints.add(ArchitectureHint("Concurrency", "Kotlin Coroutines & Flow", 1.0, "Coroutines core / Flow detected"))
        }

        // Serialization
        if (buildContent.contains("kotlinx-serialization") || buildContent.contains("plugin.serialization")) {
            hints.add(ArchitectureHint("Serialization", "Kotlinx Serialization", 1.0, "Found kotlinx.serialization plugin"))
        }

        return hints
    }

    private fun detectTestFrameworks(buildContent: String): List<String> {
        val tests = mutableListOf<String>()
        if (buildContent.contains("junit.jupiter") || buildContent.contains("useJUnitPlatform")) tests.add("JUnit 5 (Jupiter)")
        if (buildContent.contains("junit:junit")) tests.add("JUnit 4")
        if (buildContent.contains("io.mockk")) tests.add("MockK")
        if (buildContent.contains("org.mockito")) tests.add("Mockito")
        if (buildContent.contains("io.kotest")) tests.add("Kotest")
        if (buildContent.contains("androidx.test.espresso")) tests.add("Espresso")
        if (buildContent.contains("org.robolectric")) tests.add("Robolectric")
        if (buildContent.contains("kotlin(\"test\")") || buildContent.contains("kotlin-test")) tests.add("Kotlin Test")
        return tests.distinct()
    }

    private fun discoverImportantFiles(root: File, modules: List<ModuleInfo>): List<String> {
        val important = mutableListOf<String>()
        val candidates = listOf(
            "settings.gradle.kts",
            "settings.gradle",
            "build.gradle.kts",
            "build.gradle",
            "gradle.properties",
            "gradle/libs.versions.toml",
            ".kotlinpilot/rules.md"
        )
        for (c in candidates) {
            if (File(root, c).exists()) {
                important.add(c)
            }
        }
        for (m in modules) {
            val manifest = File(root, "${m.path}/src/main/AndroidManifest.xml")
            if (manifest.exists()) {
                important.add("${m.path}/src/main/AndroidManifest.xml")
            }
        }
        return important
    }
}
