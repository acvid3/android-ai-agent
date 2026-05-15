package com.androidaiagent.config

data class ToolConfig(
    val clickDelayMin: Long = 100,
    val clickDelayMax: Long = 500,
    val swipeDuration: Long = 300,
    val randomizationAmount: Float = 0.1f,
    val typingSpeed: Long = 50,
    val retryCount: Int = 3,
    val actionCooldown: Long = 500,
    val screenAnalysisInterval: Long = 1000,
    val ocrSensitivity: Float = 0.7f,
    val confidenceThreshold: Float = 0.6f,
    val failRecoveryTimeout: Long = 5000
)

data class AppConfig(
    val toolConfig: ToolConfig = ToolConfig(),
    val systemPrompt: String = "",
    val aiProvider: String = "openai",
    val apiKey: String = ""
)

class ConfigManager {
    private var config = AppConfig()

    fun getConfig(): AppConfig = config

    fun updateConfig(newConfig: AppConfig) {
        config = newConfig
    }

    fun updateToolConfig(toolConfig: ToolConfig) {
        config = config.copy(toolConfig = toolConfig)
    }

    fun updateSystemPrompt(prompt: String) {
        config = config.copy(systemPrompt = prompt)
    }
}
