package com.androidaiagent.routing

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RouteEngine(
    private val eventBus: com.androidaiagent.core.eventbus.EventBus = GlobalEventBus
) {
    private val routeDefinitions = mutableMapOf<String, RouteDefinition>()
    private val routeGraph = RouteGraph()
    private val routeHistory = mutableListOf<RouteTransition>()
    private val confidenceHistory = mutableMapOf<String, Float>()
    
    private val _currentRoute = MutableStateFlow<RouteMatch?>(null)
    val currentRoute: StateFlow<RouteMatch?> = _currentRoute.asStateFlow()
    
    private val _routeConfidence = MutableStateFlow(0f)
    val routeConfidence: StateFlow<Float> = _routeConfidence.asStateFlow()
    
    private val _isUnknownRoute = MutableStateFlow(false)
    val isUnknownRoute: StateFlow<Boolean> = _isUnknownRoute.asStateFlow()
    
    fun registerRoute(route: RouteDefinition) {
        routeDefinitions[route.name] = route
        routeGraph.registerRoute(route)
    }
    
    fun registerRoutes(routes: List<RouteDefinition>) {
        routes.forEach { registerRoute(it) }
    }
    
    fun matchRoute(uiMap: UiMap): RouteMatch {
        val previousRoute = _currentRoute.value?.routeName
        val matches = routeDefinitions.values.map { route ->
            val confidence = calculateRouteConfidence(route, uiMap)
            RouteMatch(route.name, confidence, route)
        }.filter { it.confidence > 0.3f }
        
        val bestMatch = matches.maxByOrNull { it.confidence }
        
        if (bestMatch != null && bestMatch.confidence > 0.6f) {
            handleRouteTransition(bestMatch)
            _currentRoute.value = bestMatch
            _routeConfidence.value = bestMatch.confidence
            _isUnknownRoute.value = false
            
            confidenceHistory[bestMatch.routeName] = bestMatch.confidence
            routeGraph.recordTransition(previousRoute, bestMatch.routeName, bestMatch.confidence)
            
            eventBus.tryPublish(Event.RouteMatched(bestMatch.routeName, bestMatch.confidence))
            
            return bestMatch
        }
        
        _isUnknownRoute.value = true
        _routeConfidence.value = bestMatch?.confidence ?: 0f
        
        eventBus.tryPublish(Event.RouteUnknown("No confident match found"))
        
        return bestMatch ?: RouteMatch("unknown", 0f, null)
    }
    
    private fun calculateRouteConfidence(route: RouteDefinition, uiMap: UiMap): Float {
        var score = 0f
        var totalWeight = 0f
        
        route.expectedButtons.forEach { button ->
            val found = uiMap.currentScreen.buttons.any { 
                it.text?.contains(button, ignoreCase = true) == true ||
                it.contentDescription?.contains(button, ignoreCase = true) == true
            }
            if (found) score += 1f
            totalWeight += 1f
        }
        
        route.expectedText.forEach { text ->
            val found = uiMap.currentScreen.textElements.any {
                it.text.contains(text, ignoreCase = true)
            }
            if (found) score += 1f
            totalWeight += 1f
        }
        
        if (route.packageName != null) {
            val packageMatch = uiMap.currentScreen.packageName == route.packageName
            if (packageMatch) score += 2f
            totalWeight += 2f
        }
        
        if (route.activityName != null) {
            val activityMatch = uiMap.currentScreen.activityName == route.activityName
            if (activityMatch) score += 2f
            totalWeight += 2f
        }
        
        return if (totalWeight > 0) score / totalWeight else 0f
    }
    
    private fun handleRouteTransition(match: RouteMatch) {
        val previousRoute = _currentRoute.value?.routeName
        if (previousRoute != match.routeName) {
            val transition = RouteTransition(
                from = previousRoute,
                to = match.routeName,
                timestamp = System.currentTimeMillis(),
                confidence = match.confidence
            )
            routeHistory.add(transition)
            
            if (routeHistory.size > 50) {
                routeHistory.removeAt(0)
            }
            
            eventBus.tryPublish(Event.RouteTransition(previousRoute, match.routeName))
        }
    }
    
    fun getFallbackRoute(currentRoute: String): RouteDefinition? {
        val route = routeDefinitions[currentRoute] ?: return null
        return routeDefinitions[route.parentRoute]
    }
    
    fun getChildRoutes(routeName: String): List<RouteDefinition> {
        return routeDefinitions.values.filter { it.parentRoute == routeName }
    }
    
    fun getRouteHistory(): List<RouteTransition> {
        return routeHistory.toList()
    }
    
    fun getRouteConfidence(routeName: String): Float {
        return confidenceHistory[routeName] ?: 0f
    }
    
    fun clearHistory() {
        routeHistory.clear()
        confidenceHistory.clear()
    }

    fun getRouteGraph(): RouteGraph = routeGraph
}

data class RouteDefinition(
    val name: String,
    val description: String,
    val purpose: String = description,
    val packageName: String? = null,
    val activityName: String? = null,
    val expectedButtons: List<String> = emptyList(),
    val expectedText: List<String> = emptyList(),
    val parentRoute: String? = null,
    val childRoutes: List<String> = emptyList(),
    val availableActions: List<String> = emptyList(),
    val navigationTargets: Map<String, String> = emptyMap()
)

data class RouteMatch(
    val routeName: String,
    val confidence: Float,
    val routeDefinition: RouteDefinition?
)

data class RouteTransition(
    val from: String?,
    val to: String,
    val timestamp: Long,
    val confidence: Float
)
