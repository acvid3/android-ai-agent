package com.androidaiagent.perception

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.ui.model.UiScreen
import java.util.LinkedHashMap

data class UiGraphNode(
    val id: String,
    val packageName: String,
    val activityName: String?,
    val routeName: String?,
    val lastSeenAt: Long,
    val screenHash: String?
)

data class UiGraphEdge(
    val from: String,
    val to: String,
    val count: Int,
    val lastSeenAt: Long
)

class PersistentUiGraph(
    private val maxNodes: Int = 200
) {
    private val nodes = LinkedHashMap<String, UiGraphNode>(maxNodes, 0.75f, true)
    private val edges = mutableMapOf<String, MutableMap<String, UiGraphEdge>>()
    private var previousNodeId: String? = null

    fun update(uiMap: UiMap, routeName: String? = uiMap.detectedRoute) {
        val screen = uiMap.currentScreen
        val nodeId = nodeIdFor(screen)
        nodes[nodeId] = UiGraphNode(
            id = nodeId,
            packageName = screen.packageName,
            activityName = screen.activityName,
            routeName = routeName,
            lastSeenAt = System.currentTimeMillis(),
            screenHash = screen.screenshotHash
        )

        previousNodeId?.let { from ->
            val row = edges.getOrPut(from) { mutableMapOf() }
            val existing = row[nodeId]
            row[nodeId] = if (existing == null) {
                UiGraphEdge(from, nodeId, 1, System.currentTimeMillis())
            } else {
                existing.copy(count = existing.count + 1, lastSeenAt = System.currentTimeMillis())
            }
        }

        previousNodeId = nodeId
        prune()
    }

    fun getNode(screen: UiScreen): UiGraphNode? = nodes[nodeIdFor(screen)]

    fun getRecentRoutes(): List<String> {
        return nodes.values.mapNotNull { it.routeName }.distinct()
    }

    fun getEdgeSummaries(): List<UiGraphEdge> {
        return edges.values.flatMap { it.values }
    }

    fun clear() {
        nodes.clear()
        edges.clear()
        previousNodeId = null
    }

    private fun prune() {
        while (nodes.size > maxNodes) {
            val firstKey = nodes.entries.firstOrNull()?.key ?: break
            nodes.remove(firstKey)
            edges.remove(firstKey)
        }
    }

    private fun nodeIdFor(screen: UiScreen): String {
        return buildString {
            append(screen.packageName)
            append('|')
            append(screen.activityName ?: "")
            append('|')
            append(screen.screenshotHash ?: "")
        }
    }
}
