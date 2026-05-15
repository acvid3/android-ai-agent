package com.androidaiagent.action

import com.androidaiagent.ai.ActionType
import com.androidaiagent.accessibility.AccessibilityService
import com.androidaiagent.accessibility.AccessibilityInteractionEngine
import android.graphics.Rect

class ActionExecutor(private val accessibilityService: AccessibilityService) {
    private val interactionEngine = AccessibilityInteractionEngine(accessibilityService)
    
    suspend fun execute(action: ActionType, target: String?, parameters: Map<String, Any>) {
        when (action) {
            ActionType.TAP -> executeTap(target, parameters)
            ActionType.SWIPE -> executeSwipe(parameters)
            ActionType.BACK -> executeBack()
            ActionType.WAIT -> executeWait(parameters)
            ActionType.TYPE -> executeType(target, parameters)
            ActionType.LONG_PRESS -> executeLongPress(target, parameters)
            ActionType.NOOP -> Unit
        }
    }

    private fun executeTap(target: String?, parameters: Map<String, Any>) {
        val x = parameters["x"] as? Float ?: 0f
        val y = parameters["y"] as? Float ?: 0f
        val bounds = parameters["bounds"] as? Rect
        if (bounds != null && interactionEngine.tapByBounds(bounds)) return
        if (!interactionEngine.performGestureTap(x, y)) {
            interactionEngine.fallbackShellInput(target.orEmpty())
        }
    }

    private fun executeSwipe(parameters: Map<String, Any>) {
        val x1 = parameters["x1"] as? Float ?: 0f
        val y1 = parameters["y1"] as? Float ?: 0f
        val x2 = parameters["x2"] as? Float ?: 0f
        val y2 = parameters["y2"] as? Float ?: 0f
        val duration = parameters["duration"] as? Long ?: 300L
        if (!interactionEngine.performGestureSwipe(x1, y1, x2, y2, duration)) {
            accessibilityService.performSwipe(x1, y1, x2, y2, duration)
        }
    }

    private fun executeBack() {
        accessibilityService.performBack()
    }

    private suspend fun executeWait(parameters: Map<String, Any>) {
        val duration = parameters["duration"] as? Long ?: 1000L
        kotlinx.coroutines.delay(duration)
    }

    private fun executeType(target: String?, parameters: Map<String, Any>) {
        val text = parameters["text"] as? String ?: target.orEmpty()
        val node = parameters["node"] as? android.view.accessibility.AccessibilityNodeInfo
        if (!interactionEngine.inputText(node, text)) {
            interactionEngine.fallbackShellInput(text)
        }
    }

    private fun executeLongPress(target: String?, parameters: Map<String, Any>) {
        val x = parameters["x"] as? Float ?: 0f
        val y = parameters["y"] as? Float ?: 0f
        if (!interactionEngine.performLongPress(x, y, parameters["duration"] as? Long ?: 500L)) {
            accessibilityService.performClick(x, y)
        }
    }
}
