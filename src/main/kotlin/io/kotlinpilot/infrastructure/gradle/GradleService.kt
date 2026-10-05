package io.kotlinpilot.infrastructure.gradle

import io.kotlinpilot.core.ai.AIToolParameter
import io.kotlinpilot.core.tools.AgentTool
import io.kotlinpilot.core.tools.ToolInput
import io.kotlinpilot.core.tools.ToolResult
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService
import java.io.File

data class GradleResult(
    val success: Boolean,
    val exitCode: Int,
    val task: String,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
)

class GradleService(
    private val terminalService: SafeTerminalService,
    private val workspaceRoot: File
) {
    private fun getGradleExecutable(): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val gradlewBat = File(workspaceRoot, "gradlew.bat")
        val gradlewSh = File(workspaceRoot, "gradlew")
        
        return when {
            isWindows && gradlewBat.exists() -> "gradlew.bat"
            !isWindows && gradlewSh.exists() -> "./gradlew"
            else -> "gradle"
        }
    }

    suspend fun runTask(task: String, extraArgs: List<String> = emptyList(), timeoutSeconds: Long = 300L): GradleResult {
        val exec = getGradleExecutable()
        val cmd = buildString {
            append(exec)
            append(" ")
            append(task)
            if (extraArgs.isNotEmpty()) {
                append(" ")
                append(extraArgs.joinToString(" "))
            }
        }

        val result = terminalService.execute(cmd, timeoutSeconds)
        return GradleResult(
            success = result.success,
            exitCode = result.exitCode,
            task = task,
            stdout = result.stdout,
            stderr = result.stderr,
            durationMs = result.durationMs
        )
    }

    suspend fun build(): GradleResult = runTask("build")
    suspend fun test(testFilter: String? = null): GradleResult {
        val args = if (!testFilter.isNullOrBlank()) listOf("--tests", testFilter) else emptyList()
        return runTask("test", args)
    }
    suspend fun check(): GradleResult = runTask("check")
    suspend fun lint(): GradleResult = runTask("lint")
}

class RunGradleTool(private val gradleService: GradleService) : AgentTool {
    override val name: String = "run_gradle"
    override val description: String = "Runs a Gradle build task (e.g. 'build', 'assemble', 'check', 'clean') using gradlew."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("task", "string", "Gradle task to run, e.g. 'build' or 'assemble'", required = true),
        AIToolParameter("extraArgs", "string", "Additional Gradle arguments (e.g. '--stacktrace')", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val task = input.getRequired("task")
            val extraArgsStr = input.getOptional("extraArgs")
            val extraArgs = extraArgsStr?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()

            val res = gradleService.runTask(task, extraArgs)
            val output = buildString {
                if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                if (res.stderr.isNotBlank()) append("STDERR:\n").append(res.stderr).append("\n")
                append("Task :").append(task).append(" finished with exit code ").append(res.exitCode)
                append(" in ").append(res.durationMs).append("ms")
            }

            if (res.success) {
                ToolResult.success(output, mapOf("exitCode" to res.exitCode.toString(), "task" to task))
            } else {
                ToolResult.failure("Gradle task '$task' failed with exit code ${res.exitCode}", output, mapOf("exitCode" to res.exitCode.toString(), "task" to task))
            }
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to execute Gradle task")
        }
    }
}

class RunTestsTool(private val gradleService: GradleService) : AgentTool {
    override val name: String = "run_tests"
    override val description: String = "Executes the test suite or specific tests using Gradle."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("testFilter", "string", "Optional class or method filter (e.g. 'com.example.MyTest')", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val filter = input.getOptional("testFilter")
            val res = gradleService.test(filter)
            val output = buildString {
                if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                if (res.stderr.isNotBlank()) append("STDERR:\n").append(res.stderr).append("\n")
                append("Tests finished with exit code ").append(res.exitCode)
            }

            if (res.success) {
                ToolResult.success(output, mapOf("exitCode" to res.exitCode.toString()))
            } else {
                ToolResult.failure("Tests failed with exit code ${res.exitCode}", output, mapOf("exitCode" to res.exitCode.toString()))
            }
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to run tests")
        }
    }
}

class RunLintTool(private val gradleService: GradleService) : AgentTool {
    override val name: String = "run_lint"
    override val description: String = "Executes linting / static analysis tasks in the project."
    override val parameters: List<AIToolParameter> = emptyList()

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val res = gradleService.lint()
            val output = buildString {
                if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                if (res.stderr.isNotBlank()) append("STDERR:\n").append(res.stderr).append("\n")
            }
            if (res.success) {
                ToolResult.success(output)
            } else {
                ToolResult.failure("Lint failed with exit code ${res.exitCode}", output)
            }
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to run lint")
        }
    }
}
