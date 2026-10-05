package io.kotlinpilot.infrastructure.groq

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotlinpilot.core.ai.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger("GroqAIProvider")

@Serializable
internal data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("tools") val tools: List<GroqToolDefinition>? = null,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null
)

@Serializable
internal data class GroqMessage(
    val role: String,
    val content: String? = null,
    val name: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<GroqToolCall>? = null
)

@Serializable
internal data class GroqToolCall(
    val id: String,
    val type: String = "function",
    val function: GroqFunctionCall
)

@Serializable
internal data class GroqFunctionCall(
    val name: String,
    val arguments: String
)

@Serializable
internal data class GroqToolDefinition(
    val type: String = "function",
    val function: GroqFunctionDefinition
)

@Serializable
internal data class GroqFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

@Serializable
internal data class GroqChatResponse(
    val id: String? = null,
    val choices: List<GroqChoice> = emptyList(),
    val usage: GroqUsage? = null,
    val model: String? = null
)

@Serializable
internal data class GroqChoice(
    val index: Int = 0,
    val message: GroqMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
internal data class GroqUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

class GroqAIProvider(
    private val apiKey: String,
    private val defaultModel: String = "llama-3.3-70b-versatile",
    private val baseUrl: String = "https://api.groq.com/openai/v1",
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
                isLenient = true
            })
        }
    }
) : AIProvider {
    override val name: String = "groq"

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "GROQ_API_KEY is not configured. Please set the GROQ_API_KEY environment variable or in .env file."
            )
        }

        val targetModel = request.model ?: defaultModel

        val groqMessages = request.messages.map { msg ->
            when (msg.role) {
                AIRole.SYSTEM -> GroqMessage(role = "system", content = msg.content)
                AIRole.USER -> GroqMessage(role = "user", content = msg.content)
                AIRole.ASSISTANT -> GroqMessage(
                    role = "assistant",
                    content = msg.content,
                    toolCalls = msg.toolCalls?.map { tc ->
                        GroqToolCall(
                            id = tc.id,
                            type = "function",
                            function = GroqFunctionCall(name = tc.name, arguments = tc.argumentsJson)
                        )
                    }
                )
                AIRole.TOOL -> GroqMessage(
                    role = "tool",
                    content = msg.toolResult?.output ?: msg.content ?: "",
                    name = msg.toolResult?.name,
                    toolCallId = msg.toolResult?.toolCallId
                )
            }
        }

        val groqTools = if (request.tools.isNotEmpty()) {
            request.tools.map { toolDef ->
                val properties = mutableMapOf<String, JsonElement>()
                val requiredList = mutableListOf<JsonPrimitive>()

                toolDef.parameters.forEach { param ->
                    val paramObj = mutableMapOf<String, JsonElement>(
                        "type" to JsonPrimitive(param.type),
                        "description" to JsonPrimitive(param.description)
                    )
                    if (param.enumValues != null) {
                        paramObj["enum"] = JsonArray(param.enumValues.map { JsonPrimitive(it) })
                    }
                    properties[param.name] = JsonObject(paramObj)
                    if (param.required) {
                        requiredList.add(JsonPrimitive(param.name))
                    }
                }

                val schemaObj = mutableMapOf<String, JsonElement>(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(properties),
                    "required" to JsonArray(requiredList)
                )

                GroqToolDefinition(
                    type = "function",
                    function = GroqFunctionDefinition(
                        name = toolDef.name,
                        description = toolDef.description,
                        parameters = JsonObject(schemaObj)
                    )
                )
            }
        } else null

        val groqPayload = GroqChatRequest(
            model = targetModel,
            messages = groqMessages,
            tools = groqTools,
            temperature = request.temperature,
            maxTokens = request.maxTokens
        )

        val httpResponse: HttpResponse = try {
            httpClient.post("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(groqPayload)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to communicate with Groq API endpoint $baseUrl" }
            throw RuntimeException("Groq API network request failed: ${e.message}", e)
        }

        if (!httpResponse.status.isSuccess()) {
            val errorBody = httpResponse.bodyAsText()
            logger.error { "Groq API error HTTP ${httpResponse.status.value}: $errorBody" }
            throw RuntimeException("Groq API returned error HTTP ${httpResponse.status.value}: $errorBody")
        }

        val rawResponseString = httpResponse.bodyAsText()
        val parsed: GroqChatResponse = jsonSerializer.decodeFromString(rawResponseString)

        val firstChoice = parsed.choices.firstOrNull()
            ?: throw IllegalStateException("Groq API returned an empty choices list")

        val responseMessage = firstChoice.message
        val role = when (responseMessage.role.lowercase()) {
            "system" -> AIRole.SYSTEM
            "user" -> AIRole.USER
            "tool" -> AIRole.TOOL
            else -> AIRole.ASSISTANT
        }

        val domainToolCalls = responseMessage.toolCalls?.map { tc ->
            AIToolCall(
                id = tc.id,
                name = tc.function.name,
                argumentsJson = tc.function.arguments
            )
        }

        val domainMessage = AIMessage(
            role = role,
            content = responseMessage.content,
            toolCalls = domainToolCalls
        )

        val usage = parsed.usage?.let {
            AIUsage(
                promptTokens = it.promptTokens,
                completionTokens = it.completionTokens,
                totalTokens = it.totalTokens
            )
        }

        return AIResponse(
            message = domainMessage,
            finishReason = firstChoice.finishReason,
            usage = usage,
            model = parsed.model ?: targetModel
        )
    }
}
