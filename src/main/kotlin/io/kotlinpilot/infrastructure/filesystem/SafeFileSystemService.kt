package io.kotlinpilot.infrastructure.filesystem

import io.kotlinpilot.core.security.WorkspaceSecurityPolicy
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SafeFileSystemService(
    val securityPolicy: WorkspaceSecurityPolicy
) {
    fun listFiles(relativePath: String = "", maxDepth: Int = 3): List<String> {
        val targetPath = securityPolicy.validateAndResolvePath(relativePath)
        if (!targetPath.exists()) {
            throw IllegalArgumentException("Path does not exist: $relativePath")
        }

        val result = mutableListOf<String>()
        val root = securityPolicy.getWorkspaceRoot()

        Files.walk(targetPath, maxDepth).use { stream ->
            stream.forEach { path ->
                if (path != targetPath) {
                    val rel = root.relativize(path).toString().replace('\\', '/')
                    // Skip hidden directories like .git and .gradle
                    if (!rel.startsWith(".git") && !rel.startsWith(".gradle") && !rel.contains("/.git") && !rel.contains("/.gradle") && !rel.contains("/build/") && !rel.startsWith("build/")) {
                        val isDir = path.isDirectory()
                        result.add(if (isDir) "$rel/" else rel)
                    }
                }
            }
        }
        return result.sorted()
    }

    fun readFile(relativePath: String, startLine: Int? = null, endLine: Int? = null): String {
        val targetPath = securityPolicy.validateAndResolvePath(relativePath)
        if (!targetPath.exists()) {
            throw IllegalArgumentException("File does not exist: $relativePath")
        }
        if (targetPath.isDirectory()) {
            throw IllegalArgumentException("Path is a directory, not a file: $relativePath")
        }

        val lines = targetPath.toFile().readLines()
        val sLine = (startLine ?: 1).coerceAtLeast(1)
        val eLine = (endLine ?: lines.size).coerceAtMost(lines.size)

        if (sLine > lines.size) {
            return ""
        }

        val subList = lines.subList(sLine - 1, eLine)
        return subList.mapIndexed { idx, line -> "${sLine + idx}: $line" }.joinToString("\n")
    }

    fun writeFile(relativePath: String, content: String): Long {
        val targetPath = securityPolicy.validateAndResolvePath(relativePath)
        val parent = targetPath.parent
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent)
        }
        targetPath.writeText(content)
        return targetPath.toFile().length()
    }

    fun editFile(relativePath: String, oldContent: String, newContent: String): Boolean {
        val targetPath = securityPolicy.validateAndResolvePath(relativePath)
        if (!targetPath.exists() || targetPath.isDirectory()) {
            throw IllegalArgumentException("Target file does not exist: $relativePath")
        }

        val current = targetPath.readText()
        if (!current.contains(oldContent)) {
            return false
        }

        val updated = current.replace(oldContent, newContent)
        targetPath.writeText(updated)
        return true
    }

    fun createDirectory(relativePath: String): Boolean {
        val targetPath = securityPolicy.validateAndResolvePath(relativePath)
        if (!targetPath.exists()) {
            Files.createDirectories(targetPath)
            return true
        }
        return false
    }

    fun deleteFile(relativePath: String): Boolean {
        val targetPath = securityPolicy.validateAndResolvePath(relativePath)
        if (targetPath == securityPolicy.getWorkspaceRoot()) {
            throw IllegalArgumentException("Cannot delete workspace root directory")
        }
        if (targetPath.exists()) {
            return targetPath.toFile().deleteRecursively()
        }
        return false
    }

    fun searchCode(query: String, relativePath: String = "", extension: String? = null): List<String> {
        val targetPath = securityPolicy.validateAndResolvePath(relativePath)
        if (!targetPath.exists()) {
            return emptyList()
        }

        val results = mutableListOf<String>()
        val root = securityPolicy.getWorkspaceRoot()

        Files.walk(targetPath, 10).use { stream ->
            stream.filter { !it.isDirectory() }.forEach { file ->
                val rel = root.relativize(file).toString().replace('\\', '/')
                if (!rel.startsWith(".git") && !rel.startsWith(".gradle") && !rel.contains("/build/") && !rel.startsWith("build/")) {
                    if (extension == null || file.fileName.toString().endsWith(extension)) {
                        val lines = file.toFile().readLines()
                        lines.forEachIndexed { idx, line ->
                            if (line.contains(query, ignoreCase = true)) {
                                results.add("$rel:${idx + 1}: ${line.trim()}")
                            }
                        }
                    }
                }
            }
        }
        return results
    }
}
