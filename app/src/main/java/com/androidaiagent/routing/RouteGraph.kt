package com.androidaiagent.routing

import kotlin.math.max
import kotlin.math.pow
import java.util.ArrayDeque

class RouteGraph {
    private val routes = mutableMapOf<String, RouteDefinition>()
    private val transitions = mutableMapOf<String, MutableMap<String, RouteEdge>>()
    private val invalidTransitions = mutableSetOf<RouteEdgeKey>()

    fun registerRoute(route: RouteDefinition) {
        routes[route.name] = route
        transitions.getOrPut(route.name) { mutableMapOf() }
    }

    fun registerRoutes(routeList: List<RouteDefinition>) {
        routeList.forEach { registerRoute(it) }
    }

    fun recordTransition(from: String?, to: String, confidence: Float) {
        if (from == null) return
        val key = RouteEdgeKey(from, to)
        val edgesFrom = transitions.getOrPut(from) { mutableMapOf() }
        val existing = edgesFrom[to]
        edgesFrom[to] = if (existing == null) {
            RouteEdge(from, to, 1, confidence, System.currentTimeMillis())
        } else {
            existing.copy(
                count = existing.count + 1,
                confidence = max(existing.confidence, confidence),
                lastVisitedAt = System.currentTimeMillis()
            )
        }
        invalidTransitions.remove(key)
    }

    fun invalidateTransition(from: String, to: String) {
        invalidTransitions.add(RouteEdgeKey(from, to))
    }

    fun isValidTransition(from: String?, to: String): Boolean {
        if (from == null) return true
        if (invalidTransitions.contains(RouteEdgeKey(from, to))) return false
        return routes[from]?.navigationTargets?.contains(to) == true ||
            transitions[from]?.containsKey(to) == true
    }

    fun shortestPath(from: String, to: String): List<String> {
        if (from == to) return listOf(from)

        val queue = ArrayDeque<List<String>>()
        val visited = mutableSetOf<String>()
        queue.add(listOf(from))
        visited.add(from)

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val current = path.last()
            val neighbors = neighborsOf(current)

            for (neighbor in neighbors) {
                if (neighbor in visited) continue
                val nextPath = path + neighbor
                if (neighbor == to) return nextPath
                visited.add(neighbor)
                queue.add(nextPath)
            }
        }

        return emptyList()
    }

    fun backtrack(route: String): String? {
        val inbound = transitions.values.flatMap { it.values }
            .filter { it.to == route }
            .maxByOrNull { it.count }
        return inbound?.from
    }

    fun navigationMemory(route: String): List<RouteEdge> {
        return transitions[route]?.values?.sortedByDescending { it.count } ?: emptyList()
    }

    fun confidenceDecay(route: String, target: String): Float {
        val edge = transitions[route]?.get(target) ?: return 0f
        val ageMinutes = (System.currentTimeMillis() - edge.lastVisitedAt) / 60000f
        return (edge.confidence * (0.98f.toDouble().pow(ageMinutes.toDouble()).toFloat())).coerceAtLeast(0f)
    }

    fun getGraphContext(currentRoute: String?): RouteGraphContext {
        if (currentRoute == null) {
            return RouteGraphContext(null, emptyList(), emptyList())
        }

        return RouteGraphContext(
            currentRoute = currentRoute,
            outboundTransitions = navigationMemory(currentRoute),
            inboundTransitions = transitions.values.flatMap { it.values }
                .filter { it.to == currentRoute }
                .sortedByDescending { it.count }
        )
    }

    private fun neighborsOf(route: String): List<String> {
        val direct = transitions[route]?.keys.orEmpty()
        val declared = routes[route]?.navigationTargets?.keys.orEmpty()
        return (direct + declared).distinct().filter { isValidTransition(route, it) }
    }
}

data class RouteEdge(
    val from: String,
    val to: String,
    val count: Int,
    val confidence: Float,
    val lastVisitedAt: Long
)

data class RouteEdgeKey(
    val from: String,
    val to: String
)

data class RouteGraphContext(
    val currentRoute: String?,
    val outboundTransitions: List<RouteEdge>,
    val inboundTransitions: List<RouteEdge>
)
