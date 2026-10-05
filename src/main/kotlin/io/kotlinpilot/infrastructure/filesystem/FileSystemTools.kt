package io.kotlinpilot.infrastructure.filesystem

import io.kotlinpilot.core.ai.AIToolParameter
import io.kotlinpilot.core.tools.AgentTool
import io.kotlinpilot.core.tools.ToolInput
import io.kotlinpilot.core.tools.ToolResult

class ListFilesTool(private val fs: SafeFileSystemService) : AgentTool {
    override val name: String = "list_files"
    override val description: String = "Lists files and subdirectories in the project workspace."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("path", "string", "Relative directory path (defaults to root)", required = false),
        AIToolParameter("maxDepth", "string", "Maximum directory depth to traverse (defaults to 3)", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val path = input.getOptional("path") ?: ""
            val maxDepth = input.getOptional("maxDepth")?.toIntOrNull() ?: 3
            val files = fs.listFiles(path, maxDepth)
            ToolResult.success(files.joinToString("\n"))
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to list files")
        }
    }
}

class ReadFileTool(private val fs: SafeFileSystemService) : AgentTool {
    override val name: String = "read_file"
    override val description: String = "Reads content of a file within the workspace, with optional line range."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("path", "string", "Relative file path to read", required = true),
        AIToolParameter("startLine", "string", "1-indexed starting line number", required = false),
        AIToolParameter("endLine", "string", "1-indexed ending line number", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val path = input.getRequired("path")
            val startLine = input.getOptional("startLine")?.toIntOrNull()
            val endLine = input.getOptional("endLine")?.toIntOrNull()
            val content = fs.readFile(path, startLine, endLine)
            ToolResult.success(content)
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to read file")
        }
    }
}

class WriteFileTool(private val fs: SafeFileSystemService) : AgentTool {
    override val name: String = "write_file"
    override val description: String = "Writes entire content to a file. Overwrites if file exists, creates parents if needed."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("path", "string", "Relative file path to write", required = true),
        AIToolParameter("content", "string", "Full content to write to the file", required = true)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val path = input.getRequired("path")
            val content = input.getRequired("content")
            val bytes = fs.writeFile(path, content)
            ToolResult.success("Successfully wrote $bytes bytes to $path")
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to write file")
        }
    }
}

class EditFileTool(private val fs: SafeFileSystemService) : AgentTool {
    override val name: String = "edit_file"
    override val description: String = "Replaces exact target content with new replacement content in an existing file."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("path", "string", "Relative file path to edit", required = true),
        AIToolParameter("oldContent", "string", "Exact existing string to replace", required = true),
        AIToolParameter("newContent", "string", "Replacement string", required = true)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val path = input.getRequired("path")
            val oldContent = input.getRequired("oldContent")
            val newContent = input.getRequired("newContent")
            val modified = fs.editFile(path, oldContent, newContent)
            if (modified) {
                ToolResult.success("Successfully updated $path")
            } else {
                ToolResult.failure("Target string 'oldContent' was not found in $path")
            }
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to edit file")
        }
    }
}

class CreateDirectoryTool(private val fs: SafeFileSystemService) : AgentTool {
    override val name: String = "create_directory"
    override val description: String = "Creates a directory within the workspace."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("path", "string", "Relative directory path to create", required = true)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val path = input.getRequired("path")
            fs.createDirectory(path)
            ToolResult.success("Directory created or already exists: $path")
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to create directory")
        }
    }
}

class DeleteFileTool(private val fs: SafeFileSystemService) : AgentTool {
    override val name: String = "delete_file"
    override val description: String = "Deletes a file or directory within the workspace."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("path", "string", "Relative file or directory path to delete", required = true)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val path = input.getRequired("path")
            val deleted = fs.deleteFile(path)
            if (deleted) {
                ToolResult.success("Successfully deleted $path")
            } else {
                ToolResult.failure("File does not exist or could not be deleted: $path")
            }
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to delete file")
        }
    }
}

class SearchCodeTool(private val fs: SafeFileSystemService) : AgentTool {
    override val name: String = "search_code"
    override val description: String = "Searches for text or symbol occurrences in workspace files."
    override val parameters: List<AIToolParameter> = listOf(
        AIToolParameter("query", "string", "Text or symbol to search for", required = true),
        AIToolParameter("path", "string", "Relative directory to search within (optional)", required = false),
        AIToolParameter("extension", "string", "File extension filter, e.g. .kt or .gradle.kts (optional)", required = false)
    )

    override suspend fun execute(input: ToolInput): ToolResult {
        return try {
            val query = input.getRequired("query")
            val path = input.getOptional("path") ?: ""
            val extension = input.getOptional("extension")
            val results = fs.searchCode(query, path, extension)
            if (results.isEmpty()) {
                ToolResult.success("No matches found for '$query'")
            } else {
                ToolResult.success(results.joinToString("\n"))
            }
        } catch (e: Exception) {
            ToolResult.failure(e.message ?: "Failed to search code")
        }
    }
}
