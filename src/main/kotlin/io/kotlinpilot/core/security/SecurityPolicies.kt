package io.kotlinpilot.core.security

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class SecurityException(message: String) : RuntimeException(message)

class WorkspaceSecurityPolicy(
    private val workspaceRoot: Path
) {
    private val normalizedRoot: Path = workspaceRoot.toAbsolutePath().normalize()

    private val forbiddenPatterns = listOf(
        Regex(".*\\.env(\\..*)?$", RegexOption.IGNORE_CASE),
        Regex(".*id_rsa.*", RegexOption.IGNORE_CASE),
        Regex(".*\\.ssh[/\\\\].*", RegexOption.IGNORE_CASE),
        Regex(".*\\.aws[/\\\\].*", RegexOption.IGNORE_CASE),
        Regex(".*\\.gnupg[/\\\\].*", RegexOption.IGNORE_CASE),
        Regex(".*\\.git[/\\\\]config", RegexOption.IGNORE_CASE)
    )

    fun validateAndResolvePath(relativePathOrAbsolute: String): Path {
        val candidate = if (Paths.get(relativePathOrAbsolute).isAbsolute) {
            Paths.get(relativePathOrAbsolute).normalize()
        } else {
            normalizedRoot.resolve(relativePathOrAbsolute).normalize()
        }

        if (!candidate.startsWith(normalizedRoot)) {
            throw SecurityException("Access denied: Path '$relativePathOrAbsolute' escapes workspace '$normalizedRoot'")
        }

        val relativeToRoot = normalizedRoot.relativize(candidate).toString()
        for (pattern in forbiddenPatterns) {
            if (pattern.matches(relativeToRoot) || pattern.matches(candidate.toString())) {
                throw SecurityException("Access denied: Access to sensitive file '$relativeToRoot' is forbidden by security policy")
            }
        }

        return candidate
    }

    fun isPathAllowed(path: String): Boolean {
        return try {
            validateAndResolvePath(path)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getWorkspaceRoot(): Path = normalizedRoot
}

class CommandSecurityPolicy {
    private val allowedBinaryPrefixes = listOf(
        "git",
        "gradlew",
        "./gradlew",
        ".\\gradlew",
        "gradlew.bat",
        "gradle",
        "java",
        "kotlinc"
    )

    private val forbiddenTokens = listOf(
        "sudo",
        "su",
        "rm -rf /",
        "rmdir /s /q c:",
        "format",
        "mkfs",
        "shutdown",
        "reboot",
        "curl | sh",
        "curl | bash",
        "wget | sh",
        "wget | bash",
        "> /dev/sda",
        ":(){ :|:& };:"
    )

    fun validateCommand(commandLine: String): String {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) {
            throw SecurityException("Command cannot be empty")
        }

        val lower = trimmed.lowercase()
        for (forbidden in forbiddenTokens) {
            if (lower.contains(forbidden)) {
                throw SecurityException("Security violation: Command contains forbidden pattern '$forbidden'")
            }
        }

        val binary = trimmed.split(Regex("\\s+")).first()
        val isAllowed = allowedBinaryPrefixes.any { prefix ->
            binary == prefix || binary.endsWith("/$prefix") || binary.endsWith("\\$prefix")
        }

        if (!isAllowed) {
            throw SecurityException("Security violation: Binary '$binary' is not on the allowed execution list (${allowedBinaryPrefixes.joinToString()})")
        }

        return trimmed
    }
}
