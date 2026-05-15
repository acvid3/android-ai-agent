package com.androidaiagent.learning

import com.androidaiagent.routing.RouteDefinition
import com.androidaiagent.routing.RouteGraph
import com.androidaiagent.routing.RouteTransition

class RouteLearningEngine(
    private val routeGraph: RouteGraph
) {
    fun analyzeTransitions(transitions: List<RouteTransition>): RouteLearningReport {
        val counts = transitions.groupingBy { "${it.from ?: "start"}->${it.to}" }.eachCount()
        val repeated = counts.filterValues { it >= 2 }
        val probabilities = counts.mapValues { (_, count) ->
            count.toFloat() / transitions.size.coerceAtLeast(1)
        }

        return RouteLearningReport(
            repeatedTransitions = repeated,
            transitionProbabilities = probabilities
        )
    }

    fun suggestRoutes(transitions: List<RouteTransition>): List<RouteDefinition> {
        val grouped = transitions.groupBy { it.to }
        return grouped.mapNotNull { (routeName, items) ->
            if (items.size < 2) return@mapNotNull null
            RouteDefinition(
                name = routeName,
                description = "Auto-learned route from repeated transitions",
                packageName = null,
                activityName = null,
                expectedButtons = emptyList(),
                expectedText = emptyList(),
                parentRoute = null,
                childRoutes = emptyList(),
                availableActions = emptyList(),
                navigationTargets = emptyMap()
            )
        }
    }
}

data class RouteLearningReport(
    val repeatedTransitions: Map<String, Int>,
    val transitionProbabilities: Map<String, Float>
)
