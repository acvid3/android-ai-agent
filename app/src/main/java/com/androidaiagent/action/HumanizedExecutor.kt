package com.androidaiagent.action

import com.androidaiagent.ai.ActionType
import com.androidaiagent.accessibility.AccessibilityService
import com.androidaiagent.humanization.HumanizationEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HumanizedExecutor(
    private val accessibilityService: AccessibilityService,
    private val humanizationEngine: HumanizationEngine = HumanizationEngine()
) {
    private val _lastActionTime = MutableStateFlow(0L)
    val lastActionTime: StateFlow<Long> = _lastActionTime.asStateFlow()
    
    private val _actionCount = MutableStateFlow(0)
    val actionCount: StateFlow<Int> = _actionCount.asStateFlow()
    
    private var actionCountInWindow = 0
    private var windowStartTime = System.currentTimeMillis()
    
    suspend fun execute(action: ActionType, target: String?, parameters: Map<String, Any>) {
        val pacingDelay = calculatePacingDelay()
        if (pacingDelay > 0) {
            delay(pacingDelay)
        }
        
        when (action) {
            ActionType.TAP -> executeHumanizedTap(target, parameters)
            ActionType.SWIPE -> executeHumanizedSwipe(parameters)
            ActionType.BACK -> executeHumanizedBack()
            ActionType.WAIT -> executeHumanizedWait(parameters)
            ActionType.TYPE -> executeHumanizedType(target, parameters)
            ActionType.LONG_PRESS -> executeHumanizedLongPress(target, parameters)
            ActionType.NOOP -> Unit
        }
        
        updateActionTracking()
    }
    
    private suspend fun executeHumanizedTap(target: String?, parameters: Map<String, Any>) {
        val baseX = parameters["x"] as? Float ?: 0f
        val baseY = parameters["y"] as? Float ?: 0f
        
        val (x, y) = humanizationEngine.randomizeTapOffset(baseX, baseY)
        val tapDelay = humanizationEngine.adaptiveDelay(100L, ActionType.TAP)
        
        delay(tapDelay)
        accessibilityService.performClick(x, y)
    }
    
    private suspend fun executeHumanizedSwipe(parameters: Map<String, Any>) {
        val x1 = parameters["x1"] as? Float ?: 0f
        val y1 = parameters["y1"] as? Float ?: 0f
        val x2 = parameters["x2"] as? Float ?: 0f
        val y2 = parameters["y2"] as? Float ?: 0f
        val baseDuration = parameters["duration"] as? Long ?: 300L
        
        val duration = humanizationEngine.variableSwipeDuration(baseDuration)
        val curvePoints = humanizationEngine.generateSwipeCurve(x1, y1, x2, y2, duration)
        
        for (point in curvePoints) {
            accessibilityService.performClick(point.first, point.second)
            delay(16)
        }
    }
    
    private suspend fun executeHumanizedBack() {
        val backDelay = humanizationEngine.adaptiveDelay(100L, ActionType.BACK)
        delay(backDelay)
        accessibilityService.performBack()
    }
    
    private suspend fun executeHumanizedWait(parameters: Map<String, Any>) {
        val baseDuration = parameters["duration"] as? Long ?: 1000L
        val idleDuration = humanizationEngine.simulateIdleBehavior(baseDuration, baseDuration * 2)
        delay(idleDuration)
    }
    
    private suspend fun executeHumanizedType(target: String?, parameters: Map<String, Any>) {
        val text = parameters["text"] as? String ?: ""
        val baseSpeed = parameters["typingSpeed"] as? Long ?: 50L
        
        for (char in text) {
            val charDelay = humanizationEngine.randomTypingSpeed(baseSpeed)
            delay(charDelay)
            
        }
    }
    
    private suspend fun executeHumanizedLongPress(target: String?, parameters: Map<String, Any>) {
        val baseX = parameters["x"] as? Float ?: 0f
        val baseY = parameters["y"] as? Float ?: 0f
        val duration = parameters["duration"] as? Long ?: 500L
        
        val (x, y) = humanizationEngine.randomizeTapOffset(baseX, baseY)
        val pressDelay = humanizationEngine.adaptiveDelay(100L, ActionType.LONG_PRESS)
        
        delay(pressDelay)
        accessibilityService.performClick(x, y)
        delay(duration)
    }
    
    private fun calculatePacingDelay(): Long {
        val now = System.currentTimeMillis()
        val windowDuration = 60000L
        
        if (now - windowStartTime > windowDuration) {
            actionCountInWindow = 0
            windowStartTime = now
        }
        
        return humanizationEngine.calculateInteractionPacing(actionCountInWindow, windowDuration)
    }
    
    private fun updateActionTracking() {
        _lastActionTime.value = System.currentTimeMillis()
        actionCountInWindow++
        _actionCount.value = actionCountInWindow
    }
    
    fun resetTracking() {
        actionCountInWindow = 0
        windowStartTime = System.currentTimeMillis()
        _actionCount.value = 0
    }
}
