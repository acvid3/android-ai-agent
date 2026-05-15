package com.androidaiagent.ai

class AIProviderFactory {
    companion object {
        fun createProvider(
            provider: String,
            apiKey: String = "",
            endpoint: String = "",
            model: String = ""
        ): AIProvider {
            return when (provider.lowercase()) {
                "openai" -> createOpenAI(apiKey)
                "claude" -> createClaude(apiKey)
                "ollama" -> createOllama(
                    baseUrl = if (endpoint.isNotBlank()) endpoint else "http://localhost:11434",
                    model = if (model.isNotBlank()) model else "llama2"
                )
                "lmstudio" -> createLMStudio(
                    baseUrl = if (endpoint.isNotBlank()) endpoint else "http://localhost:1234",
                    model = if (model.isNotBlank()) model else "local-model"
                )
                else -> createCustom(endpoint, emptyMap())
            }
        }

        fun createOpenAI(apiKey: String): AIProvider {
            return OpenAIProvider(apiKey)
        }
        
        fun createClaude(apiKey: String): AIProvider {
            return ClaudeProvider(apiKey)
        }
        
        fun createOllama(baseUrl: String = "http://localhost:11434", model: String = "llama2"): AIProvider {
            return OllamaProvider(baseUrl, model)
        }
        
        fun createLMStudio(baseUrl: String = "http://localhost:1234", model: String = "local-model"): AIProvider {
            return LMStudioProvider(baseUrl, model)
        }
        
        fun createCustom(endpoint: String, headers: Map<String, String>): AIProvider {
            return CustomAIProvider(endpoint, headers)
        }
    }
}

class OllamaProvider(
    private val baseUrl: String,
    private val model: String
) : AIProvider {
    override suspend fun makeDecision(request: AIRequest): AIResponse {
        return AIResponse(ActionType.NOOP, null, "Ollama not implemented")
    }
}

class LMStudioProvider(
    private val baseUrl: String,
    private val model: String
) : AIProvider {
    override suspend fun makeDecision(request: AIRequest): AIResponse {
        return AIResponse(ActionType.NOOP, null, "LM Studio not implemented")
    }
}

class CustomAIProvider(
    private val endpoint: String,
    private val headers: Map<String, String>
) : AIProvider {
    override suspend fun makeDecision(request: AIRequest): AIResponse {
        return AIResponse(ActionType.NOOP, null, "Custom provider not implemented")
    }
}
