package com.androidaiagent.routing

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.ui.model.UiScreen
import com.androidaiagent.state.contextmemory.ContextMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.androidaiagent.routing.RouteScoreMatch

class EnhancedRoutingEngine(
    private val contextMemory: ContextMemory
) {
    private val routes = mutableMapOf<String, Route>()
    private val routeHistory = mutableListOf<EnhancedRouteTransition>()
    private val fallbackRoutes = mutableMapOf<String, String>()
    
    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute: StateFlow<String?> = _currentRoute.asStateFlow()
    
    private val _routeConfidence = MutableStateFlow(0f)
    val routeConfidence: StateFlow<Float> = _routeConfidence.asStateFlow()
    
    fun addRoute(route: Route) {
        routes[route.name] = route
    }
    
    fun addRoutes(routeList: List<Route>) {
        routeList.forEach { addRoute(it) }
    }
    
    fun setFallbackRoute(from: String, to: String) {
        fallbackRoutes[from] = to
    }
    
    fun identifyRoute(uiMap: UiMap): RouteScoreMatch {
        val screen = uiMap.currentScreen
        val scores = mutableMapOf<String, Float>()
        
        for (route in routes.values) {
            val score = calculateRouteScore(route, screen)
            if (score > 0.3f) {
                scores[route.name] = score
            }
        }
        
        val bestMatch = scores.maxByOrNull { it.value }
        
        if (bestMatch != null && bestMatch.value > 0.5f) {
            val previousRoute = _currentRoute.value
            _currentRoute.value = bestMatch.key
            _routeConfidence.value = bestMatch.value
            
            if (previousRoute != bestMatch.key) {
                recordTransition(previousRoute, bestMatch.key)
                contextMemory.recordRouteTransition(previousRoute, bestMatch.key)
            }
            
            return RouteScoreMatch(
                route = routes[bestMatch.key]!!,
                confidence = bestMatch.value,
                matchedElements = getMatchedElements(routes[bestMatch.key]!!, screen)
            )
        }
        
        return handleUnknownRoute(screen)
    }
    
    private fun calculateRouteScore(route: Route, screen: UiScreen): Float {
        var score = 0f
        var totalChecks = 0
        
        for (button in route.expectedButtons) {
            totalChecks++
            val found = screen.buttons.any { 
                it.text?.contains(button, ignoreCase = true) == true ||
                it.contentDescription?.contains(button, ignoreCase = true) == true
            }
            if (found) score += 1f
        }
        
        for (text in route.expectedText) {
            totalChecks++
            val found = screen.textElements.any { 
                it.text.contains(text, ignoreCase = true)
            }
            if (found) score += 1f
        }
        
        if (totalChecks > 0) {
            return score / totalChecks
        }
        
        return 0f
    }
    
    private fun getMatchedElements(route: Route, screen: UiScreen): List<String> {
        val matched = mutableListOf<String>()
        
        route.expectedButtons.forEach { button ->
            if (screen.buttons.any { 
                it.text?.contains(button, ignoreCase = true) == true ||
                it.contentDescription?.contains(button, ignoreCase = true) == true
            }) {
                matched.add(button)
            }
        }
        
        route.expectedText.forEach { text ->
            if (screen.textElements.any { it.text.contains(text, ignoreCase = true) }) {
                matched.add(text)
            }
        }
        
        return matched
    }
    
    private fun handleUnknownRoute(screen: UiScreen): RouteScoreMatch {
        val previousRoute = _currentRoute.value
        val fallback = previousRoute?.let { fallbackRoutes[it] }
        
        if (fallback != null && routes.containsKey(fallback)) {
            _currentRoute.value = fallback
            _routeConfidence.value = 0.3f
            recordTransition(previousRoute, fallback)
            
            return RouteScoreMatch(
                route = routes[fallback]!!,
                confidence = 0.3f,
                matchedElements = emptyList()
            )
        }
        
        _currentRoute.value = "unknown"
        _routeConfidence.value = 0f
        
        return RouteScoreMatch(
            route = Route(
                name = "unknown",
                description = "Unknown screen",
                purpose = "Unknown",
                expectedButtons = emptyList(),
                expectedText = emptyList(),
                availableActions = emptyList(),
                navigationTargets = emptyList()
            ),
            confidence = 0f,
            matchedElements = emptyList()
        )
    }
    
    private fun recordTransition(from: String?, to: String) {
        routeHistory.add(EnhancedRouteTransition(from, to, System.currentTimeMillis()))
        
        if (routeHistory.size > 100) {
            routeHistory.removeAt(0)
        }
    }
    
    fun getRoute(name: String): Route? {
        return routes[name]
    }
    
    fun getAllRoutes(): List<Route> {
        return routes.values.toList()
    }
    
    fun getTransitionHistory(): List<EnhancedRouteTransition> {
        return routeHistory
    }
    
    fun canTransition(from: String, to: String): Boolean {
        val fromRoute = routes[from] ?: return false
        return to in fromRoute.navigationTargets
    }
    
    fun getNavigationTargets(routeName: String): List<String> {
        return routes[routeName]?.navigationTargets ?: emptyList()
    }
    
    fun reset() {
        _currentRoute.value = null
        _routeConfidence.value = 0f
        routeHistory.clear()
    }
}

data class EnhancedRouteTransition(
    val from: String?,
    val to: String,
    val timestamp: Long
)
