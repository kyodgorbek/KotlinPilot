package io.kotlinpilot.core.sandbox

import io.kotlinpilot.infrastructure.terminal.ProcessResult
import java.nio.file.Path

enum class SandboxType {
    LOCAL,
    DOCKER,
    MICRO_VM
}

interface ExecutionEnvironment {
    val type: SandboxType
    suspend fun executeCommand(command: String, timeoutSeconds: Long = 120L): ProcessResult
    suspend fun prepareWorkspace(workspacePath: Path)
    suspend fun cleanup()
}
