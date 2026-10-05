package io.kotlinpilot.infrastructure.terminal

import io.kotlinpilot.core.ai.AIToolParameter
import io.kotlinpilot.core.security.CommandSecurityPolicy
import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import io.kotlinpilot.core.tools.AgentTool
import io.kotlinpilot.core.tools.ToolInput
import io.kotlinpilot.core.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.TimeUnit

data class ProcessResult(
    val success: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
)

class SafeTerminalService(
    private val workspacePolicy: WorkspaceSecurityPolicy,
    private val commandPolicy: CommandSecurityPolicy = CommandSecurityPolicy(),
    private val defaultTimeoutSeconds: Long = 120L
) {
    suspend fun execute(
        commandLine: String,
        timeoutSeconds: Long = defaultTimeoutSeconds,
        workingDirRelative: String = ""
    ): ProcessResult = withContext(Dispatchers.IO) {
        val validatedCommand = try {
            commandPolicy.validateCommand(commandLine)
        } catch (e: Exception) {
            return@withContext ProcessResult(
                success = false,
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Command security policy violation",
                durationMs = 0
            )
        }

        val workingDir = workspacePolicy.validateAndResolvePath(workingDirRelative).toFile()
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val processBuilder = if (isWindows) {
            ProcessBuilder("cmd.exe", "/c", validatedCommand)
        } else {
            ProcessBuilder("sh", "-c", validatedCommand)
        }

        processBuilder.directory(workingDir)
        // Set environment variables safely
        processBuilder.environment().put("PAGER", "cat")
        processBuilder.environment().put("CI", "true")

        val startTime = System.currentTimeMillis()
        val process = try {
            processBuilder.start()
        } catch (e: Exception) {
            return@withContext ProcessResult(
                success = false,
                exitCode = -1,
                stdout = "",
                stderr = "Failed to launch process: ${e.message}",
                durationMs = 0
            )
        }

        val stdoutFuture = process.inputStream.bufferedReader().useLines { it.toList() }
        val stderrFuture = process.errorStream.bufferedReader().useLines { it.toList() }

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        val duration = System.currentTimeMillis() - startTime

        if (!completed) {
            process.destroyForcibly()
            return@withContext ProcessResult(
                success = false,
                exitCode = -1,
                stdout = stdoutFuture.joinToString("\n"),
                stderr = "Command timed out after $timeoutSeconds seconds",
                durationMs = duration
            )
        }

        val exitCode = process.exitValue()
        val stdout = stdoutFuture.joinToString("\n")
        val stderr = stderrFuture.joinToString("\n")

        ProcessResult(
            success = exitCode == 0,
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            durationMs = duration
        )
    }
}

class TerminalTool(
    private val terminalService: SafeTerminalService
) : AgentTool {
    override val name: String = "execute_command"
    override val description: String = "Executes an allowlisted terminal command (e.g. git, gradlew, java) within the workspace."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("command", "string", "Command to execute (e.g., './gradlew build' or 'git status')", required = true),
        AIToolParameter("timeoutSeconds", "string", "Timeout in seconds (optional)", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val command = input.getRequired("command")
            val timeout = input.getOptional("timeoutSeconds")?.toLongOrNull() ?: 120L
            val result = terminalService.execute(command, timeout)
            
            val outputDetails = buildString {
                if (result.stdout.isNotBlank()) append(result.stdout).append("\n")
                if (result.stderr.isNotBlank()) append("STDERR:\n").append(result.stderr).append("\n")
                append("Exit Code: ").append(result.exitCode)
                append(" (").append(result.durationMs).append("ms)")
            }

            if (result.success) {
                ToolResult.success(outputDetails, mapOf("exitCode" to result.exitCode.toString()))
            } else {
                ToolResult.failure("Command exited with code ${result.exitCode}", outputDetails, mapOf("exitCode" to result.exitCode.toString()))
            }
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to execute command")
        }
    }
}
