package io.kotlinpilot.core.ai

class FakeAIProvider(
    val scriptedResponses: MutableList<AIResponse> = mutableListOf()
) : AIProvider {
    override val name: String = "fake"
    val recordedRequests: MutableList<AIRequest> = mutableListOf()

    fun enqueueResponse(response: AIResponse) {
        scriptedResponses.add(response)
    }

    fun enqueueAssistantText(text: String) {
        scriptedResponses.add(
            AIResponse(
                message = AIMessage(role = AIRole.ASSISTANT, content = text),
                finishReason = "stop"
            )
        )
    }

    fun enqueueToolCalls(toolCalls: List<AIToolCall>, messageContent: String? = null) {
        scriptedResponses.add(
            AIResponse(
                message = AIMessage(
                    role = AIRole.ASSISTANT,
                    content = messageContent,
                    toolCalls = toolCalls
                ),
                finishReason = "tool_calls"
            )
        )
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        recordedRequests.add(request)
        if (scriptedResponses.isNotEmpty()) {
            return scriptedResponses.removeAt(0)
        }

        // Default heuristic response if none scripted:
        val lastMessage = request.messages.lastOrNull()
        return if (lastMessage?.role == AIRole.TOOL) {
            AIResponse(
                message = AIMessage(
                    role = AIRole.ASSISTANT,
                    content = "Task analysis and verification completed successfully."
                ),
                finishReason = "stop"
            )
        } else {
            AIResponse(
                message = AIMessage(
                    role = AIRole.ASSISTANT,
                    content = "Acknowledged request. No further actions required."
                ),
                finishReason = "stop"
            )
        }
    }
}
