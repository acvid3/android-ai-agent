package com.androidaiagent.ai

import com.androidaiagent.routing.RouteGraph
import com.androidaiagent.state.contextmemory.ContextSummary
import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.vision.SemanticUiContext

class AiContextBuilder(
    private val routeGraph: RouteGraph? = null
) {
    fun build(
        uiMap: UiMap,
        task: String,
        systemPrompt: String,
        contextSummary: ContextSummary,
        semanticContext: SemanticUiContext? = null
    ): AIRequest {
        val currentScreen = uiMap.currentScreen
        val routeContext = routeGraph?.getGraphContext(uiMap.detectedRoute).orEmpty()
        val importantText = currentScreen.textElements
            .take(8)
            .joinToString("\n") { it.text }
        val buttonLabels = currentScreen.buttons
            .take(10)
            .mapNotNull { it.text ?: it.contentDescription }
        val semanticSummary = semanticContext?.summary.orEmpty()

        return AIRequest(
            currentRoute = uiMap.detectedRoute,
            visibleText = listOf(
                "screen=${currentScreen.packageName}/${currentScreen.activityName ?: "unknown"}",
                "semantic=$semanticSummary",
                importantText
            ).filter { it.isNotBlank() }.joinToString("\n"),
            detectedButtons = buttonLabels,
            ocrResults = currentScreen.textElements.joinToString("\n") { "${it.text}:${it.confidence}" },
            previousActions = contextSummary.recentActions.map { "${it.action} on ${it.target}" },
            currentTask = task,
            systemPrompt = buildSystemPrompt(systemPrompt, contextSummary, routeContext, semanticContext),
            availableRoutes = routeContext.outboundTransitions.map { it.to }
        )
    }

    private fun buildSystemPrompt(
        systemPrompt: String,
        contextSummary: ContextSummary,
        routeContext: com.androidaiagent.routing.RouteGraphContext,
        semanticContext: SemanticUiContext?
    ): String {
        val chunks = mutableListOf<String>()
        if (systemPrompt.isNotBlank()) chunks += systemPrompt
        contextSummary.currentGoal?.let { chunks += "goal=$it" }
        if (routeContext.currentRoute != null) {
            chunks += "route=${routeContext.currentRoute}"
            chunks += "routeNeighbors=${routeContext.outboundTransitions.joinToString { it.to }}"
        }
        semanticContext?.let {
            chunks += "ui=${it.summary}"
            if (it.modalDetected) chunks += "modal=true"
        }
        return chunks.joinToString("\n")
    }
}

private fun com.androidaiagent.routing.RouteGraphContext?.orEmpty(): com.androidaiagent.routing.RouteGraphContext {
    return this ?: com.androidaiagent.routing.RouteGraphContext(null, emptyList(), emptyList())
}
