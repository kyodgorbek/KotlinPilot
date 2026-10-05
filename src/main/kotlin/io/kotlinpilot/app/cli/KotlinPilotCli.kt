package io.kotlinpilot.app.cli

import io.kotlinpilot.core.agent.CodeReviewer
import io.kotlinpilot.core.agent.KotlinPilotAgent
import io.kotlinpilot.core.ai.AIProvider
import io.kotlinpilot.core.ai.FakeAIProvider
import io.kotlinpilot.core.domain.Task
import io.kotlinpilot.core.domain.TaskMode
import io.kotlinpilot.core.security.CommandSecurityPolicy
import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import io.kotlinpilot.core.tools.ToolRegistry
import io.kotlinpilot.infrastructure.analyzer.RepositoryAnalyzer
import io.kotlinpilot.infrastructure.config.KotlinPilotConfig
import io.kotlinpilot.infrastructure.filesystem.*
import io.kotlinpilot.infrastructure.git.*
import io.kotlinpilot.infrastructure.gradle.*
import io.kotlinpilot.infrastructure.groq.GroqAIProvider
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths

class KotlinPilotCli(
    private val config: KotlinPilotConfig = KotlinPilotConfig.load()
) {
    private val workspacePolicy = WorkspaceSecurityPolicy(config.projectWorkspace)
    private val commandPolicy = CommandSecurityPolicy()
    private val terminalService = SafeTerminalService(workspacePolicy, commandPolicy, config.commandTimeoutSeconds)
    private val fsService = SafeFileSystemService(workspacePolicy)
    private val gradleService = GradleService(terminalService, config.projectWorkspace.toFile())
    private val gitService = GitService(terminalService)
    private val analyzer = RepositoryAnalyzer(workspacePolicy)
    private val codeReviewer = CodeReviewer(gitService)

    private val toolRegistry = ToolRegistry().apply {
        register(ListFilesTool(fsService))
        register(ReadFileTool(fsService))
        register(WriteFileTool(fsService))
        register(EditFileTool(fsService))
        register(CreateDirectoryTool(fsService))
        register(DeleteFileTool(fsService))
        register(SearchCodeTool(fsService))
        register(RunGradleTool(gradleService))
        register(RunTestsTool(gradleService))
        register(RunLintTool(gradleService))
        register(GitStatusTool(gitService))
        register(GitDiffTool(gitService))
        register(GitBranchTool(gitService))
        register(GitCheckoutTool(gitService))
        register(GitCommitTool(gitService))
    }

    private fun createAIProvider(overrideProvider: String? = null, overrideModel: String? = null): AIProvider {
        val providerName = overrideProvider ?: config.aiProvider
        val model = overrideModel ?: config.groqModel
        return when (providerName.lowercase()) {
            "fake" -> FakeAIProvider()
            "groq" -> GroqAIProvider(
                apiKey = config.groqApiKey,
                defaultModel = model
            )
            else -> GroqAIProvider(
                apiKey = config.groqApiKey,
                defaultModel = model
            )
        }
    }

    fun analyze() {
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  KotlinPilot - Autonomous Kotlin & Android Agent")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Analyzing workspace: ${config.projectWorkspace} ...\n")

        val analysis = analyzer.analyze()

        println("  Project Name:     ${analysis.projectName}")
        println("  Kotlin:           ${if (analysis.isKotlin) "✓ Detected (${analysis.kotlinVersion ?: "auto"})" else "✗ Not detected"}")
        println("  Gradle:           ${if (analysis.isGradle) "✓ Detected (${analysis.gradleVersion ?: "auto"})" else "✗ Not detected"}")
        println("  Android Compose:  ${if (analysis.composeEnabled && !analysis.composeMultiplatformEnabled) "✓ Yes (Jetpack Compose)" else if (analysis.composeMultiplatformEnabled) "✓ Yes (Compose Multiplatform)" else "✗ No"}")
        println("  Kotlin Multiplatform: ${if (analysis.kmpEnabled) "✓ Yes (${if (analysis.kmpTargets.isNotEmpty()) analysis.kmpTargets.joinToString(", ") else "common"})" else "✗ No"}")
        println("  Modules (${analysis.modules.size}):")
        analysis.modules.forEach { mod ->
            val tags = mutableListOf<String>()
            if (mod.isAndroid) tags.add("Android")
            if (mod.isComposeMultiplatform) tags.add("CMP") else if (mod.isCompose) tags.add("Compose")
            if (mod.isKmp) tags.add("KMP")
            println("    - :${mod.name} (${tags.ifEmpty { listOf("JVM") }.joinToString(", ")})")
        }

        if (analysis.architectureHints.isNotEmpty()) {
            println("\n  Architecture & Conventions:")
            analysis.architectureHints.forEach { hint ->
                println("    - [${hint.category}] ${hint.pattern} (${hint.details})")
            }
        }

        if (analysis.testFrameworks.isNotEmpty()) {
            println("\n  Test Frameworks:   ${analysis.testFrameworks.joinToString(", ")}")
        }

        if (!analysis.customRules.isNullOrBlank()) {
            println("\n  Project Rules (.kotlinpilot/rules.md): Loaded")
        }

        println("\nAnalysis complete.")
    }

    fun runTask(
        taskDescription: String,
        mode: TaskMode = config.defaultMode,
        provider: String? = null,
        model: String? = null
    ) = runBlocking {
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  KotlinPilot - Task Execution")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Task:      $taskDescription")
        println("Workspace: ${config.projectWorkspace}")
        println("Mode:      $mode")
        println("Provider:  ${provider ?: config.aiProvider} (${model ?: config.groqModel})")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        val aiProvider = createAIProvider(provider, model)
        val agent = KotlinPilotAgent(aiProvider, toolRegistry, analyzer, codeReviewer)

        val task = Task(
            description = taskDescription,
            workspacePath = config.projectWorkspace.toString(),
            mode = mode,
            maxIterations = config.maxAgentIterations
        )

        val result = agent.executeTask(task) { event ->
            val icon = when (event.type.name) {
                "ANALYSIS_STARTED", "ANALYSIS_COMPLETED" -> "🔍"
                "PLAN_CREATED" -> "📋"
                "TOOL_STARTED" -> "⚙️ "
                "TOOL_COMPLETED" -> "✓ "
                "REVIEW_STARTED", "REVIEW_COMPLETED" -> "🧐"
                "TASK_COMPLETED" -> "🎉"
                "TASK_FAILED" -> "❌"
                else -> "• "
            }
            println("$icon [${event.type}] ${event.message}")
        }

        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  Execution Summary")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Status:          ${result.task.status}")
        println("Iterations:      ${result.iterationsCount} / ${config.maxAgentIterations}")
        println("Tools Executed:  ${result.toolExecutionsCount}")
        if (result.reviewResult != null) {
            println("Review Summary:  ${result.reviewResult.summary}")
            result.reviewResult.findings.forEach { finding ->
                println("  [${finding.severity}] ${finding.file}:${finding.line ?: "-"} - ${finding.description}")
            }
        }
        println("\nFinal Response:\n${result.finalResponse}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }

    fun status() = runBlocking {
        println("Workspace: ${config.projectWorkspace}")
        val branch = gitService.getCurrentBranch()
        println("Current Git Branch: $branch\n")
        val status = gitService.status()
        println("Git Status:\n$status")
    }

    fun diff(path: String? = null) = runBlocking {
        val diff = gitService.diff(path)
        println("Git Diff:\n$diff")
    }

    fun review() = runBlocking {
        println("Running Code Review on uncommitted changes in ${config.projectWorkspace}...")
        val analysis = analyzer.analyze()
        val review = codeReviewer.review(analysis)
        println("\nReview Status: ${if (review.approved) "APPROVED ✓" else "CHANGES REQUESTED ✗"}")
        println("Summary:       ${review.summary}\n")
        if (review.findings.isNotEmpty()) {
            println("Findings:")
            review.findings.forEach { f ->
                println("  [${f.severity}] ${f.file}:${f.line ?: "-"} - ${f.description}")
                println("    Recommendation: ${f.recommendation}")
            }
        }
    }

    fun startServer(port: Int = 8080) {
        val taskManager = io.kotlinpilot.server.TaskManager(
            agentFactory = { provider, model ->
                val aiProvider = createAIProvider(provider, model)
                KotlinPilotAgent(aiProvider, toolRegistry, analyzer, codeReviewer)
            },
            workspacePath = config.projectWorkspace.toString()
        )

        val server = io.kotlinpilot.server.KotlinPilotServer(
            config = config,
            taskManager = taskManager,
            analyzer = analyzer,
            gitService = gitService,
            codeReviewer = codeReviewer,
            port = port
        )
        server.start(wait = true)
    }
}
