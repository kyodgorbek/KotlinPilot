package io.kotlinpilot.infrastructure.config

import io.kotlinpilot.core.domain.TaskMode
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

data class KotlinPilotConfig(
    val aiProvider: String = System.getenv("AI_PROVIDER") ?: System.getProperty("AI_PROVIDER") ?: "groq",
    val groqApiKey: String = System.getenv("GROQ_API_KEY") ?: System.getProperty("GROQ_API_KEY") ?: "",
    val groqModel: String = System.getenv("GROQ_MODEL") ?: System.getProperty("GROQ_MODEL") ?: "llama-3.3-70b-versatile",
    val projectWorkspace: Path = resolveWorkspacePath(System.getenv("PROJECT_WORKSPACE") ?: System.getProperty("PROJECT_WORKSPACE")),
    val maxAgentIterations: Int = ((System.getenv("MAX_AGENT_ITERATIONS") ?: System.getProperty("MAX_AGENT_ITERATIONS"))?.toIntOrNull() ?: 20).coerceIn(1, 100),
    val commandTimeoutSeconds: Long = ((System.getenv("COMMAND_TIMEOUT_SECONDS") ?: System.getProperty("COMMAND_TIMEOUT_SECONDS"))?.toLongOrNull() ?: 120L).coerceIn(5, 600),
    val defaultMode: TaskMode = when ((System.getenv("AGENT_MODE") ?: System.getProperty("AGENT_MODE"))?.uppercase()) {
        "EXPLAIN" -> TaskMode.EXPLAIN
        "AUTONOMOUS" -> TaskMode.AUTONOMOUS
        else -> TaskMode.ASSISTED
    }
) {
    companion object {
        fun load(): KotlinPilotConfig {
            loadDotEnvIfPresent()
            return KotlinPilotConfig()
        }

        private fun resolveWorkspacePath(configured: String?): Path {
            if (!configured.isNullOrBlank()) {
                val path = Paths.get(configured).toAbsolutePath().normalize()
                return path
            }
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        }

        private fun loadDotEnvIfPresent() {
            val envFile = File(".env")
            if (envFile.exists() && envFile.isFile) {
                envFile.readLines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        val parts = trimmed.split("=", limit = 2)
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
                        if (System.getenv(key) == null && System.getProperty(key) == null) {
                            System.setProperty(key, value)
                        }
                    }
                }
            }
        }
    }
}
