package io.kotlinpilot.infrastructure.sandbox

import io.kotlinpilot.core.sandbox.ExecutionEnvironment
import io.kotlinpilot.core.sandbox.SandboxType
import io.kotlinpilot.infrastructure.terminal.ProcessResult
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService
import java.nio.file.Path

class LocalExecutionEnvironment(
    private val terminalService: SafeTerminalService
) : ExecutionEnvironment {
    override val type: SandboxType = SandboxType.LOCAL

    override suspend fun executeCommand(command: String, timeoutSeconds: Long): ProcessResult {
        return terminalService.execute(command, timeoutSeconds)
    }

    override suspend fun prepareWorkspace(workspacePath: Path) {
        // Local environment uses the direct workspace path
    }

    override suspend fun cleanup() {
        // No ephemeral container teardown required
    }
}
