package io.kotlinpilot.server

import io.kotlinpilot.core.agent.CodeReviewer
import io.kotlinpilot.core.agent.KotlinPilotAgent
import io.kotlinpilot.core.ai.FakeAIProvider
import io.kotlinpilot.core.domain.TaskMode
import io.kotlinpilot.core.domain.TaskStatus
import io.kotlinpilot.core.security.CommandSecurityPolicy
import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import io.kotlinpilot.core.tools.ToolRegistry
import io.kotlinpilot.infrastructure.analyzer.RepositoryAnalyzer
import io.kotlinpilot.infrastructure.filesystem.*
import io.kotlinpilot.infrastructure.git.GitService
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TaskManagerTest {

    @Test
    fun `test submit task and record events`(@TempDir tempDir: Path) = runTest {
        val workspacePolicy = WorkspaceSecurityPolicy(tempDir)
        val commandPolicy = CommandSecurityPolicy()
        val terminalService = SafeTerminalService(workspacePolicy, commandPolicy)
        val fsService = SafeFileSystemService(workspacePolicy)
        val gitService = GitService(terminalService)
        val analyzer = RepositoryAnalyzer(workspacePolicy)
        val codeReviewer = CodeReviewer(gitService)

        val toolRegistry = ToolRegistry().apply {
            register(ListFilesTool(fsService))
            register(ReadFileTool(fsService))
            register(WriteFileTool(fsService))
        }

        val taskManager = TaskManager(
            agentFactory = { _, _ ->
                KotlinPilotAgent(FakeAIProvider(), toolRegistry, analyzer, codeReviewer)
            },
            workspacePath = tempDir.toString()
        )

        val task = taskManager.submitTask(
            description = "Test task execution",
            mode = TaskMode.EXPLAIN
        )

        assertNotNull(task.id)
        assertEquals("Test task execution", task.description)

        val retrieved = taskManager.getTask(task.id.value)
        assertNotNull(retrieved)

        val tasksList = taskManager.listTasks()
        assertTrue(tasksList.any { it.id.value == task.id.value })
    }
}
