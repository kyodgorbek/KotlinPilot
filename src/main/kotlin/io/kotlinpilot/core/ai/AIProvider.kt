package io.kotlinpilot.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class AIRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

@Serializable
data class AIToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

@Serializable
data class AIToolResult(
    val toolCallId: String,
    val name: String,
    val output: String,
    val isError: Boolean = false
)

@Serializable
data class AIMessage(
    val role: AIRole,
    val content: String? = null,
    val toolCalls: List<AIToolCall>? = null,
    val toolResult: AIToolResult? = null
)

@Serializable
data class AIToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true,
    val enumValues: List<String>? = null
)

@Serializable
data class AIToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<AIToolParameter> = emptyList()
)

@Serializable
data class AIUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

@Serializable
data class AIRequest(
    val messages: List<AIMessage>,
    val tools: List<AIToolDefinition> = emptyList(),
    val temperature: Double = 0.2,
    val maxTokens: Int? = 4096,
    val model: String? = null
)

@Serializable
data class AIResponse(
    val message: AIMessage,
    val finishReason: String? = null,
    val usage: AIUsage? = null,
    val model: String? = null
)

interface AIProvider {
    val name: String
    suspend fun generate(request: AIRequest): AIResponse
}
