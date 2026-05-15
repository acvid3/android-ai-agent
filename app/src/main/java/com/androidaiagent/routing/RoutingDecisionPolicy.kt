package com.androidaiagent.routing

class RoutingDecisionPolicy(
    private val deterministicThreshold: Float = 0.75f,
    private val aiThreshold: Float = 0.45f
) {
    fun choose(routeMatch: RouteMatch?, aiConfidence: Float): RoutingDecision {
        val routeConfidence = routeMatch?.confidence ?: 0f
        return when {
            routeConfidence >= deterministicThreshold -> RoutingDecision.DETERMINISTIC
            routeConfidence >= aiThreshold -> RoutingDecision.HYBRID
            aiConfidence >= aiThreshold -> RoutingDecision.AI_ASSISTED
            else -> RoutingDecision.UNKNOWN
        }
    }
}

enum class RoutingDecision {
    DETERMINISTIC,
    HYBRID,
    AI_ASSISTED,
    UNKNOWN
}
