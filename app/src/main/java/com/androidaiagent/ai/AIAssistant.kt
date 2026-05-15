package com.androidaiagent.ai

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.state.contextmemory.ContextMemory
import com.androidaiagent.vision.SemanticUiInterpreter
import com.androidaiagent.core.eventbus.EventBus
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIAssistant(
    private val provider: AIProvider,
    private val contextMemory: ContextMemory,
    private val aiContextBuilder: AiContextBuilder = AiContextBuilder(),
    private val semanticUiInterpreter: SemanticUiInterpreter = SemanticUiInterpreter(),
    private val eventBus: EventBus = GlobalEventBus
) {
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _lastResponse = MutableStateFlow<AIResponse?>(null)
    val lastResponse: StateFlow<AIResponse?> = _lastResponse.asStateFlow()
    
    private val _confidence = MutableStateFlow(0f)
    val confidence: StateFlow<Float> = _confidence.asStateFlow()
    
    suspend fun assistDecision(
        uiMap: UiMap,
        currentTask: String,
        systemPrompt: String
    ): AIResponse? {
        if (_isProcessing.value) return null
        
        _isProcessing.value = true
        
        try {
            val contextSummary = contextMemory.getContextSummary()
            val semanticContext = semanticUiInterpreter.interpret(uiMap)
            
            val request = aiContextBuilder.build(
                uiMap = uiMap,
                task = currentTask,
                systemPrompt = systemPrompt,
                contextSummary = contextSummary,
                semanticContext = semanticContext
            )
            
            val requestId = "req_${System.currentTimeMillis()}"
            eventBus.tryPublish(Event.AIRequestSent(requestId, "Decision assistance"))
            
            val response = provider.makeDecision(request)
            
            eventBus.tryPublish(Event.AIResponseReceived(requestId, "${response.action} on ${response.target}"))
            
            _lastResponse.value = response
            _confidence.value = calculateConfidence(response, uiMap)
            
            return response
        } catch (e: Exception) {
            eventBus.tryPublish(Event.ErrorOccurred(e.message ?: "AI error", "AIAssistant"))
            return null
        } finally {
            _isProcessing.value = false
        }
    }
    
    suspend fun suggestActions(uiMap: UiMap, context: String): List<ActionSuggestion> {
        if (_isProcessing.value) return emptyList()
        
        _isProcessing.value = true
        
        try {
            val suggestions = mutableListOf<ActionSuggestion>()
            
            uiMap.currentScreen.buttons.forEach { button ->
                suggestions.add(ActionSuggestion(
                    action = ActionType.TAP,
                    target = button.text ?: button.contentDescription,
                    reason = "Detected button",
                    confidence = button.confidence
                ))
            }
            
            return suggestions.sortedByDescending { it.confidence }
        } finally {
            _isProcessing.value = false
        }
    }
    
    suspend fun resolveUnknownState(uiMap: UiMap): AIResponse? {
        val contextSummary = contextMemory.getContextSummary()
        val semanticContext = semanticUiInterpreter.interpret(uiMap)

        val request = aiContextBuilder.build(
            uiMap = uiMap,
            task = "resolve_unknown_state",
            systemPrompt = "You are in an unknown state. Analyze the screen and suggest how to return to a known route.",
            contextSummary = contextSummary,
            semanticContext = semanticContext
        )
        
        return provider.makeDecision(request)
    }
    
    private fun calculateConfidence(response: AIResponse, uiMap: UiMap): Float {
        val routeConfidence = uiMap.routeConfidence
        val baseConfidence = when (response.action) {
            ActionType.TAP -> 0.8f
            ActionType.SWIPE -> 0.7f
            ActionType.BACK -> 0.9f
            ActionType.WAIT -> 0.6f
            else -> 0.5f
        }
        
        return (routeConfidence + baseConfidence) / 2f
    }
}

data class ActionSuggestion(
    val action: ActionType,
    val target: String?,
    val reason: String,
    val confidence: Float
)
