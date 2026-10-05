package io.kotlinpilot.core.tools

import io.kotlinpilot.core.ai.AIToolDefinition
import io.kotlinpilot.core.ai.AIToolParameter
import kotlinx.serialization.Serializable

@Serializable
data class ToolInput(
    val arguments: Map<String, String> = emptyMap()
) {
    fun getRequired(key: String): String =
        arguments[key] ?: throw IllegalArgumentException("Missing required parameter: $key")

    fun getOptional(key: String, default: String? = null): String? =
        arguments[key] ?: default
}

@Serializable
data class ToolResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun success(output: String, metadata: Map<String, String> = emptyMap()) =
            ToolResult(success = true, output = output, metadata = metadata)

        fun failure(error: String, output: String = "", metadata: Map<String, String> = emptyMap()) =
            ToolResult(success = false, output = output, error = error, metadata = metadata)
    }
}

interface AgentTool {
    val name: String
    val description: String
    val parameters: List<AIToolParameter>

    fun getDefinition(): AIToolDefinition = AIToolDefinition(
        name = name,
        description = description,
        parameters = parameters
    )

    suspend fun execute(input: ToolInput): ToolResult
}

class ToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()

    fun register(tool: AgentTool): ToolRegistry {
        tools[tool.name] = tool
        return this
    }

    fun getTool(name: String): AgentTool? = tools[name]

    fun getAllTools(): List<AgentTool> = tools.values.toList()

    fun getToolDefinitions(): List<AIToolDefinition> = tools.values.map { it.getDefinition() }
}
