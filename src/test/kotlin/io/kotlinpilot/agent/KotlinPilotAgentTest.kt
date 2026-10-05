package io.kotlinpilot.agent

import io.kotlinpilot.core.agent.CodeReviewer
import io.kotlinpilot.core.agent.KotlinPilotAgent
import io.kotlinpilot.core.ai.*
import io.kotlinpilot.core.domain.Task
import io.kotlinpilot.core.domain.TaskEvent
import io.kotlinpilot.core.domain.TaskStatus
import io.kotlinpilot.core.security.CommandSecurityPolicy
import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import io.kotlinpilot.core.tools.ToolRegistry
import io.kotlinpilot.infrastructure.analyzer.RepositoryAnalyzer
import io.kotlinpilot.infrastructure.filesystem.*
import io.kotlinpilot.infrastructure.git.GitService
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KotlinPilotAgentTest {

    @Test
    fun `test agent executes tool loop and completes task`(@TempDir tempDir: Path) = runTest {
        val policy = WorkspaceSecurityPolicy(tempDir)
        val commandPolicy = CommandSecurityPolicy()
        val terminalService = SafeTerminalService(policy, commandPolicy)
        val fsService = SafeFileSystemService(policy)
        val gitService = GitService(terminalService)
        val analyzer = RepositoryAnalyzer(policy)
        val codeReviewer = CodeReviewer(gitService)

        // Pre-create initial file
        fsService.writeFile("settings.gradle.kts", "rootProject.name = \"Sample\"")
        fsService.writeFile("src/Greeting.kt", "fun greet() = \"Hi\"\n")

        val toolRegistry = ToolRegistry().apply {
            register(ListFilesTool(fsService))
            register(ReadFileTool(fsService))
            register(WriteFileTool(fsService))
            register(EditFileTool(fsService))
        }

        val fakeAI = FakeAIProvider()

        // 1st iteration: AI decides to read the greeting file
        fakeAI.enqueueToolCalls(
            listOf(
                AIToolCall(
                    id = "call_1",
                    name = "read_file",
                    argumentsJson = """{"path": "src/Greeting.kt"}"""
                )
            )
        )

        // 2nd iteration: AI decides to edit the greeting file
        fakeAI.enqueueToolCalls(
            listOf(
                AIToolCall(
                    id = "call_2",
                    name = "edit_file",
                    argumentsJson = """{"path": "src/Greeting.kt", "oldContent": "Hi", "newContent": "Hello World"}"""
                )
            )
        )

        // 3rd iteration: AI gives final explanation
        fakeAI.enqueueAssistantText("Updated greeting message to 'Hello World' as requested.")

        val agent = KotlinPilotAgent(fakeAI, toolRegistry, analyzer, codeReviewer)

        val task = Task(
            description = "Change greeting message to Hello World",
            workspacePath = tempDir.toString(),
            maxIterations = 10
        )

        val events = mutableListOf<TaskEvent>()
        val result = agent.executeTask(task) { events.add(it) }

        assertEquals(TaskStatus.COMPLETED, result.task.status)
        assertEquals(2, result.toolExecutionsCount)
        assertTrue(result.finalResponse.contains("Updated greeting message"))

        val content = fsService.readFile("src/Greeting.kt")
        assertTrue(content.contains("Hello World"))
        assertTrue(events.any { it.type.name == "TOOL_STARTED" })
        assertTrue(events.any { it.type.name == "TASK_COMPLETED" })
    }

    @Test
    fun `test agent terminates gracefully when max iterations reached`(@TempDir tempDir: Path) = runTest {
        val policy = WorkspaceSecurityPolicy(tempDir)
        val commandPolicy = CommandSecurityPolicy()
        val terminalService = SafeTerminalService(policy, commandPolicy)
        val fsService = SafeFileSystemService(policy)
        val gitService = GitService(terminalService)
        val analyzer = RepositoryAnalyzer(policy)
        val codeReviewer = CodeReviewer(gitService)

        val toolRegistry = ToolRegistry().apply {
            register(ListFilesTool(fsService))
        }

        val fakeAI = FakeAIProvider()
        // Continuously request tool calls without stopping
        repeat(5) {
            fakeAI.enqueueToolCalls(
                listOf(
                    AIToolCall(
                        id = "call_loop_$it",
                        name = "list_files",
                        argumentsJson = "{}"
                    )
                )
            )
        }

        val agent = KotlinPilotAgent(fakeAI, toolRegistry, analyzer, codeReviewer)

        val task = Task(
            description = "Looping task test",
            workspacePath = tempDir.toString(),
            maxIterations = 3
        )

        val result = agent.executeTask(task)
        assertEquals(TaskStatus.FAILED, result.task.status)
        assertTrue(result.finalResponse.contains("Exceeded maximum iterations"))
    }
}
