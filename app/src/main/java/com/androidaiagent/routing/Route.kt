package com.androidaiagent.routing

data class Route(
    val name: String,
    val description: String,
    val purpose: String,
    val expectedButtons: List<String>,
    val expectedText: List<String>,
    val availableActions: List<String>,
    val navigationTargets: List<String>,
    val parentRoute: String? = null,
    val childRoutes: List<String> = emptyList()
)

data class RouteScoreMatch(
    val route: Route,
    val confidence: Float,
    val matchedElements: List<String>
)

class RoutingEngine {
    private val routes = mutableListOf<Route>()

    fun addRoute(route: Route) {
        routes.add(route)
    }

    fun identifyCurrentScreen(detectedElements: List<String>, text: String): RouteScoreMatch? {
        var bestMatch: RouteScoreMatch? = null
        var bestConfidence = 0f

        for (route in routes) {
            val confidence = calculateMatchConfidence(route, detectedElements, text)
            if (confidence > bestConfidence && confidence > 0.5f) {
                bestConfidence = confidence
                bestMatch = RouteScoreMatch(route, confidence, emptyList())
            }
        }

        return bestMatch
    }

    private fun calculateMatchConfidence(route: Route, detectedElements: List<String>, text: String): Float {
        var score = 0f
        var totalChecks = 0

        for (button in route.expectedButtons) {
            totalChecks++
            if (detectedElements.any { it.contains(button, ignoreCase = true) }) {
                score += 1f
            }
        }

        for (expectedText in route.expectedText) {
            totalChecks++
            if (text.contains(expectedText, ignoreCase = true)) {
                score += 1f
            }
        }

        return if (totalChecks > 0) score / totalChecks else 0f
    }

    fun getRoute(name: String): Route? {
        return routes.find { it.name == name }
    }

    fun getAllRoutes(): List<Route> = routes
}
