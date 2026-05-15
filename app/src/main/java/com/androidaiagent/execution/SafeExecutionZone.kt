package com.androidaiagent.execution

import android.graphics.Rect
import com.androidaiagent.ai.ActionType
import com.androidaiagent.ui.model.UiMap
import java.util.ArrayDeque

class SafeExecutionZone(
    private val maxTapsPerSecond: Int = 3,
    private val forbiddenRegions: List<Rect> = emptyList(),
    private val blockedPackages: Set<String> = emptySet(),
    private val blockedActions: Set<ActionType> = setOf(ActionType.NOOP)
) {
    private val tapWindow = ArrayDeque<Long>()

    fun allows(action: ActionType, target: String?, parameters: Map<String, Any>, uiMap: UiMap?): Boolean {
        if (action in blockedActions) {
            return false
        }

        val packageName = uiMap?.currentScreen?.packageName
        if (packageName != null && packageName in blockedPackages) {
            return false
        }

        if (action == ActionType.TAP || action == ActionType.LONG_PRESS) {
            if (!passesTapRateLimit()) {
                return false
            }

            val bounds = parameters["bounds"] as? Rect
            val x = parameters["x"] as? Float
            val y = parameters["y"] as? Float
            if (bounds != null && forbiddenRegions.any { Rect.intersects(it, bounds) }) {
                return false
            }
            if (x != null && y != null && forbiddenRegions.any { it.contains(x.toInt(), y.toInt()) }) {
                return false
            }
        }

        if (action == ActionType.TYPE) {
            val text = parameters["text"] as? String ?: target.orEmpty()
            if (text.isBlank()) {
                return false
            }
        }

        return true
    }

    private fun passesTapRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        while (tapWindow.isNotEmpty() && now - tapWindow.first() > 1000L) {
            tapWindow.removeFirst()
        }
        return if (tapWindow.size >= maxTapsPerSecond) {
            false
        } else {
            tapWindow.addLast(now)
            true
        }
    }
}
