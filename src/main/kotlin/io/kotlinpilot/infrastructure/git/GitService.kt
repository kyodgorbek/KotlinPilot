package io.kotlinpilot.infrastructure.git

import io.kotlinpilot.core.ai.AIToolParameter
import io.kotlinpilot.core.tools.AgentTool
import io.kotlinpilot.core.tools.ToolInput
import io.kotlinpilot.core.tools.ToolResult
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService

class GitService(
    private val terminalService: SafeTerminalService
) {
    suspend fun status(): String {
        val result = terminalService.execute("git status --short")
        return if (result.success) result.stdout.ifBlank { "Working directory clean" } else result.stderr
    }

    suspend fun diff(path: String? = null): String {
        val cmd = if (!path.isNullOrBlank()) "git diff -- $path" else "git diff"
        val result = terminalService.execute(cmd)
        return if (result.success) result.stdout.ifBlank { "No diff" } else result.stderr
    }

    suspend fun getCurrentBranch(): String {
        val result = terminalService.execute("git branch --show-current")
        return if (result.success) result.stdout.trim() else "unknown"
    }

    suspend fun listBranches(): String {
        val result = terminalService.execute("git branch")
        return if (result.success) result.stdout else result.stderr
    }

    suspend fun createAndCheckoutBranch(branchName: String): String {
        val sanitized = branchName.replace(Regex("[^a-zA-Z0-9/_-]"), "-")
        val result = terminalService.execute("git checkout -b $sanitized")
        return if (result.success) "Switched to new branch: $sanitized" else result.stderr
    }

    suspend fun checkoutBranch(branchName: String): String {
        val result = terminalService.execute("git checkout $branchName")
        return if (result.success) "Checked out $branchName" else result.stderr
    }

    suspend fun add(path: String = "."): String {
        val result = terminalService.execute("git add $path")
        return if (result.success) "Staged changes for $path" else result.stderr
    }

    suspend fun commit(message: String): String {
        val sanitizedMsg = message.replace("\"", "\\\"").replace("\n", " ")
        val result = terminalService.execute("git commit -m \"$sanitizedMsg\"")
        return if (result.success) result.stdout else result.stderr
    }
}

class GitStatusTool(private val git: GitService) : AgentTool {
    override val name: String = "git_status"
    override val description: String = "Shows working tree status (git status)."
    override val parameters: List<AIToolParameter> = emptyList()

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val status = git.status()
            ToolResult.success(status)
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to get git status")
        }
    }
}

class GitDiffTool(private val git: GitService) : AgentTool {
    override val name: String = "git_diff"
    override val description: String = "Shows changes between commits, commit and working tree, etc."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("path", "string", "Optional relative file path to diff", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val path = input.getOptional("path")
            val diff = git.diff(path)
            ToolResult.success(diff)
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to get git diff")
        }
    }
}

class GitBranchTool(private val git: GitService) : AgentTool {
    override val name: String = "git_branch"
    override val description: String = "Lists git branches or shows current branch."
    override val parameters: List<AIToolParameter> = emptyList()

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val branches = git.listBranches()
            val current = git.getCurrentBranch()
            ToolResult.success("Current: $current\n\n$branches")
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to list branches")
        }
    }
}

class GitCheckoutTool(private val git: GitService) : AgentTool {
    override val name: String = "git_checkout"
    override val description: String = "Switches branches or creates a new branch."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("branch", "string", "Branch name to switch to or create", required = true),
        AIToolParameter("createNew", "string", "Set to 'true' to create new branch (-b)", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val branch = input.getRequired("branch")
            val createNew = input.getOptional("createNew")?.toBoolean() ?: false
            val res = if (createNew) git.createAndCheckoutBranch(branch) else git.checkoutBranch(branch)
            ToolResult.success(res)
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to checkout branch")
        }
    }
}

class GitCommitTool(private val git: GitService) : AgentTool {
    override val name: String = "git_commit"
    override val description: String = "Stages files and records changes to the repository with a commit message."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("message", "string", "Commit message", required = true),
        AIToolParameter("path", "string", "Path to stage (defaults to '.')", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val message = input.getRequired("message")
            val path = input.getOptional("path") ?: "."
            git.add(path)
            val res = git.commit(message)
            ToolResult.success(res)
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to commit changes")
        }
    }
}
