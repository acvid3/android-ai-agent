package com.androidaiagent.prompt

class PromptManager {
    
    fun buildAIRequest(
        currentRoute: String?,
        visibleText: String,
        detectedButtons: List<String>,
        ocrResults: String,
        previousActions: List<String>,
        currentTask: String,
        systemPrompt: String,
        availableRoutes: List<String>
    ): String {
        return buildString {
            appendLine("System: $systemPrompt")
            appendLine()
            appendLine("Current Route: $currentRoute")
            appendLine("Available Routes: ${availableRoutes.joinToString(", ")}")
            appendLine()
            appendLine("Screen Information:")
            appendLine("- Visible Text: $visibleText")
            appendLine("- Detected Buttons: ${detectedButtons.joinToString(", ")}")
            appendLine("- OCR Results: $ocrResults")
            appendLine()
            appendLine("Task: $currentTask")
            appendLine("Previous Actions: ${previousActions.joinToString(", ")}")
            appendLine()
            appendLine("Respond with JSON only:")
            appendLine("{")
            appendLine("  \"action\": \"tap|swipe|back|wait|type|long_press|noop\",")
            appendLine("  \"target\": \"element_name\",")
            appendLine("  \"reason\": \"explanation\",")
            appendLine("  \"parameters\": {\"x\": 100, \"y\": 200}")
            appendLine("}")
        }
    }
}
