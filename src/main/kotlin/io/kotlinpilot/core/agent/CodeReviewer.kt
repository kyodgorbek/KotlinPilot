package io.kotlinpilot.core.agent

import io.kotlinpilot.core.domain.CodeReviewFinding
import io.kotlinpilot.core.domain.CodeReviewResult
import io.kotlinpilot.core.domain.ProjectAnalysis
import io.kotlinpilot.core.domain.Severity
import io.kotlinpilot.infrastructure.git.GitService

class CodeReviewer(
    private val gitService: GitService
) {
    suspend fun review(analysis: ProjectAnalysis): CodeReviewResult {
        val diff = gitService.diff()
        if (diff.isBlank() || diff == "No diff") {
            return CodeReviewResult(
                approved = true,
                summary = "No file modifications were made.",
                findings = emptyList()
            )
        }

        val findings = mutableListOf<CodeReviewFinding>()
        val lines = diff.lines()

        var currentFile = ""
        var addedLines = 0
        var deletedLines = 0

        for (line in lines) {
            if (line.startsWith("+++ b/")) {
                currentFile = line.removePrefix("+++ b/")
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                addedLines++
                val content = line.removePrefix("+")

                // Check for hardcoded secrets
                if (content.contains("AIzaSy", ignoreCase = true) ||
                    content.contains("gsk_", ignoreCase = true) ||
                    content.contains("ghp_", ignoreCase = true) ||
                    Regex("(?i)apiKey\\s*=\\s*\"[a-zA-Z0-9_-]{16,}\"").containsMatchIn(content)
                ) {
                    findings.add(
                        CodeReviewFinding(
                            severity = Severity.CRITICAL,
                            file = currentFile,
                            description = "Potential hardcoded secret or API key detected.",
                            recommendation = "Use environment variables or secure credential storage instead."
                        )
                    )
                }

                // Check for GlobalScope / unconstrained coroutines
                if (content.contains("GlobalScope.launch") || content.contains("GlobalScope.async")) {
                    findings.add(
                        CodeReviewFinding(
                            severity = Severity.WARNING,
                            file = currentFile,
                            description = "Use of GlobalScope detected.",
                            recommendation = "Use structured concurrency with CoroutineScope or lifecycle-aware scopes (e.g. viewModelScope)."
                        )
                    )
                }

                // Check for Thread.sleep in coroutines
                if (content.contains("Thread.sleep") && (currentFile.endsWith(".kt") || currentFile.endsWith(".kts"))) {
                    findings.add(
                        CodeReviewFinding(
                            severity = Severity.WARNING,
                            file = currentFile,
                            description = "Thread.sleep() blocks the underlying thread.",
                            recommendation = "Use delay() inside coroutine functions."
                        )
                    )
                }

                // Check for Hilt introduction if project uses Koin
                val usesKoin = analysis.architectureHints.any { it.pattern == "Koin" }
                if (usesKoin && (content.contains("@HiltAndroidApp") || content.contains("@AndroidEntryPoint") || content.contains("@Inject"))) {
                    findings.add(
                        CodeReviewFinding(
                            severity = Severity.ERROR,
                            file = currentFile,
                            description = "Project architecture standard is Koin, but Hilt annotations were added.",
                            recommendation = "Adhere to the existing dependency injection framework (Koin)."
                        )
                    )
                }
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                deletedLines++
            }
        }

        val hasCritical = findings.any { it.severity == Severity.CRITICAL || it.severity == Severity.ERROR }
        val summary = buildString {
            append("Reviewed diff: +$addedLines / -$deletedLines lines across repository. ")
            if (findings.isEmpty()) {
                append("All checks passed with no anti-pattern findings.")
            } else {
                append("${findings.size} finding(s) detected (${findings.count { it.severity == Severity.ERROR || it.severity == Severity.CRITICAL }} blocking).")
            }
        }

        return CodeReviewResult(
            approved = !hasCritical,
            summary = summary,
            findings = findings
        )
    }
}
