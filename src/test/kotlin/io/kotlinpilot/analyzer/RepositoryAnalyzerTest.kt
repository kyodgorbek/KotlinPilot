package io.kotlinpilot.analyzer

import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import io.kotlinpilot.infrastructure.analyzer.RepositoryAnalyzer
import io.kotlinpilot.infrastructure.filesystem.SafeFileSystemService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RepositoryAnalyzerTest {

    @Test
    fun `test repository analyzer detects kotlin gradle koin compose and custom rules`(@TempDir tempDir: Path) {
        val policy = WorkspaceSecurityPolicy(tempDir)
        val fs = SafeFileSystemService(policy)

        // Setup mock Gradle multi-module project
        fs.writeFile("settings.gradle.kts", """
            rootProject.name = "TestApp"
            include(":app")
            include(":core:domain")
        """.trimIndent())

        fs.writeFile("app/build.gradle.kts", """
            plugins {
                id("com.android.application")
                kotlin("android")
            }
            dependencies {
                implementation("androidx.compose.ui:ui:1.6.0")
                implementation("io.insert-koin:koin-androidx-compose:3.5.0")
                implementation("io.ktor:ktor-client-core:2.3.0")
                testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
            }
        """.trimIndent())

        fs.writeFile("core/domain/build.gradle.kts", """
            plugins {
                kotlin("jvm")
            }
        """.trimIndent())

        fs.writeFile(".kotlinpilot/rules.md", """
            # Architecture Rules
            - Prefer Koin over Hilt.
            - Use MVI architecture.
        """.trimIndent())

        val analyzer = RepositoryAnalyzer(policy)
        val analysis = analyzer.analyze()

        assertEquals(tempDir.toFile().name, analysis.projectName)
        assertTrue(analysis.isGradle)
        assertTrue(analysis.composeEnabled)
        assertEquals(2, analysis.modules.size)
        assertTrue(analysis.modules.any { it.name == "app" && it.isAndroid })
        assertTrue(analysis.architectureHints.any { it.pattern == "Koin" })
        assertTrue(analysis.architectureHints.any { it.pattern == "Jetpack Compose" })
        assertTrue(analysis.architectureHints.any { it.pattern == "Ktor Client" })
        assertNotNull(analysis.customRules)
        assertTrue(analysis.customRules!!.contains("Prefer Koin over Hilt"))
    }
}
