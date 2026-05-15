package com.androidaiagent.ai

interface AIProvider {
    suspend fun makeDecision(request: AIRequest): AIResponse
}

data class AIRequest(
    val currentRoute: String?,
    val visibleText: String,
    val detectedButtons: List<String>,
    val ocrResults: String,
    val previousActions: List<String>,
    val currentTask: String,
    val systemPrompt: String,
    val availableRoutes: List<String>
)

data class AIResponse(
    val action: ActionType,
    val target: String?,
    val reason: String,
    val parameters: Map<String, Any> = emptyMap()
)

enum class ActionType {
    TAP,
    SWIPE,
    BACK,
    WAIT,
    TYPE,
    LONG_PRESS,
    NOOP
}

class OpenAIProvider(private val apiKey: String) : AIProvider {
    override suspend fun makeDecision(request: AIRequest): AIResponse {
        // TODO: Implement OpenAI API call
        return AIResponse(ActionType.NOOP, null, "Not implemented")
    }
}

class ClaudeProvider(private val apiKey: String) : AIProvider {
    override suspend fun makeDecision(request: AIRequest): AIResponse {
        // TODO: Implement Claude API call
        return AIResponse(ActionType.NOOP, null, "Not implemented")
    }
}
