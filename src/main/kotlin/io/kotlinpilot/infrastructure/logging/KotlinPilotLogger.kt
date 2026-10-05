package io.kotlinpilot.infrastructure.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotlinpilot.core.domain.TaskId
import java.time.Instant

object KotlinPilotLogger {
    private val rawLogger = KotlinLogging.logger("KotlinPilot")

    fun log(
        taskId: TaskId? = null,
        stage: String? = null,
        tool: String? = null,
        status: String? = null,
        durationMs: Long? = null,
        message: String
    ) {
        val sanitizedMessage = maskSecrets(message)
        val builder = StringBuilder()
        if (taskId != null) builder.append("[$taskId] ")
        if (stage != null) builder.append("[$stage] ")
        if (tool != null) builder.append("[Tool: $tool] ")
        if (status != null) builder.append("($status) ")
        builder.append(sanitizedMessage)
        if (durationMs != null) builder.append(" (${durationMs}ms)")

        rawLogger.info { builder.toString() }
    }

    fun debug(message: String) {
        rawLogger.debug { maskSecrets(message) }
    }

    fun warn(message: String) {
        rawLogger.warn { maskSecrets(message) }
    }

    fun error(message: String, throwable: Throwable? = null) {
        rawLogger.error(throwable) { maskSecrets(message) }
    }

    fun maskSecrets(input: String): String {
        var masked = input
        // Mask Groq/OpenAI/GitHub API keys
        masked = masked.replace(Regex("gsk_[A-Za-z0-9_-]{20,}", RegexOption.IGNORE_CASE), "gsk_***[REDACTED]***")
        masked = masked.replace(Regex("sk-[A-Za-z0-9_-]{20,}", RegexOption.IGNORE_CASE), "sk-***[REDACTED]***")
        masked = masked.replace(Regex("ghp_[A-Za-z0-9]{20,}", RegexOption.IGNORE_CASE), "ghp_***[REDACTED]***")
        masked = masked.replace(Regex("github_pat_[A-Za-z0-9_]{20,}", RegexOption.IGNORE_CASE), "github_pat_***[REDACTED]***")
        masked = masked.replace(Regex("(?i)(api[_-]?key|secret|password|token)\\s*[:=]\\s*['\"]?([A-Za-z0-9_\\-+=]{8,})['\"]?")) { match ->
            val key = match.groupValues[1]
            "$key=***[REDACTED]***"
        }
        return masked
    }
}
