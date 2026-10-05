package io.kotlinpilot.infrastructure.git

import io.kotlinpilot.core.git.PullRequestInfo
import io.kotlinpilot.core.security.CommandSecurityPolicy
import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GitHubProviderTest {

    @Test
    fun `test simulated PR creation when unconfigured`(@TempDir tempDir: Path) = runTest {
        val workspacePolicy = WorkspaceSecurityPolicy(tempDir)
        val commandPolicy = CommandSecurityPolicy()
        val terminalService = SafeTerminalService(workspacePolicy, commandPolicy)
        val provider = GitHubProvider(terminalService)

        val pr = PullRequestInfo(
            title = "Feature: Add Ktor Server",
            body = "Closes #123",
            headBranch = "feat/ktor-server",
            baseBranch = "main"
        )

        val result = provider.createPullRequest(pr)
        assertTrue(result.isSuccess)
        val createdPr = result.getOrNull()
        assertNotNull(createdPr)
        assertNotNull(createdPr?.url)
    }
}
