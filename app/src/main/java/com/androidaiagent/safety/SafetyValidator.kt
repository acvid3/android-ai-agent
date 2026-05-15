package com.androidaiagent.safety

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.ui.model.InteractionZone
import com.androidaiagent.ai.ActionType
import com.androidaiagent.core.eventbus.EventBus
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import com.androidaiagent.core.eventbus.SafetySeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SafetyValidator(
    private val eventBus: EventBus = GlobalEventBus
) {
    private val actionHistory = mutableListOf<ActionRecord>()
    private val screenHistory = mutableListOf<String>()
    private var lastActionTime = 0L
    private var consecutiveSameActions = 0
    private var lastActionType: ActionType? = null
    
    private val _safetyStatus = MutableStateFlow(SafetyStatus.SAFE)
    val safetyStatus: StateFlow<SafetyStatus> = _safetyStatus.asStateFlow()
    
    private val maxActionsPerMinute = 60
    private val maxConsecutiveSameActions = 3
    private val freezeDetectionThreshold = 5000L
    private var lastScreenHash: String? = null
    private var screenFreezeStartTime = 0L
    
    suspend fun validateAction(
        action: ActionType,
        target: String?,
        parameters: Map<String, Any>,
        uiMap: UiMap
    ): ValidationResult {
        val now = System.currentTimeMillis()
        
        val violations = mutableListOf<SafetyViolation>()
        
        if (isInDangerousZone(parameters, uiMap)) {
            violations.add(SafetyViolation("Action in dangerous zone", SafetySeverity.HIGH))
        }
        
        if (isActionRateExceeded(now)) {
            violations.add(SafetyViolation("Action rate exceeded", SafetySeverity.MEDIUM))
        }
        
        if (isLoopingAction(action)) {
            violations.add(SafetyViolation("Potential infinite loop detected", SafetySeverity.HIGH))
        }
        
        if (isScreenFrozen(uiMap, now)) {
            violations.add(SafetyViolation("Screen freeze detected", SafetySeverity.CRITICAL))
        }
        
        if (isOutsideSafeZone(parameters, uiMap)) {
            violations.add(SafetyViolation("Action outside safe zone", SafetySeverity.MEDIUM))
        }
        
        if (violations.isNotEmpty()) {
            violations.forEach { violation ->
                eventBus.tryPublish(Event.SafetyViolation(violation.message, violation.severity))
            }
            _safetyStatus.value = SafetyStatus.UNSAFE
            return ValidationResult(false, violations)
        }
        
        recordAction(action, target, parameters, now)
        updateScreenHistory(uiMap, now)
        
        _safetyStatus.value = SafetyStatus.SAFE
        return ValidationResult(true, emptyList())
    }
    
    private fun isInDangerousZone(parameters: Map<String, Any>, uiMap: UiMap): Boolean {
        val x = parameters["x"] as? Float ?: return false
        val y = parameters["y"] as? Float ?: return false
        
        val dangerousZones = uiMap.currentScreen.getDangerousZones()
        return dangerousZones.any { it.bounds.contains(x.toInt(), y.toInt()) }
    }
    
    private fun isActionRateExceeded(now: Long): Boolean {
        val oneMinuteAgo = now - 60000
        val recentActions = actionHistory.count { it.timestamp > oneMinuteAgo }
        return recentActions >= maxActionsPerMinute
    }
    
    private fun isLoopingAction(action: ActionType): Boolean {
        if (lastActionType == action) {
            consecutiveSameActions++
            if (consecutiveSameActions >= maxConsecutiveSameActions) {
                return true
            }
        } else {
            consecutiveSameActions = 1
            lastActionType = action
        }
        return false
    }
    
    private fun isScreenFrozen(uiMap: UiMap, now: Long): Boolean {
        val currentHash = generateScreenHash(uiMap)
        
        if (currentHash == lastScreenHash) {
            if (screenFreezeStartTime == 0L) {
                screenFreezeStartTime = now
            } else if (now - screenFreezeStartTime > freezeDetectionThreshold) {
                return true
            }
        } else {
            screenFreezeStartTime = 0L
            lastScreenHash = currentHash
        }
        
        return false
    }
    
    private fun isOutsideSafeZone(parameters: Map<String, Any>, uiMap: UiMap): Boolean {
        val x = parameters["x"] as? Float ?: return false
        val y = parameters["y"] as? Float ?: return false
        
        val safeZones = uiMap.currentScreen.getSafeZones()
        if (safeZones.isEmpty()) return false
        
        return !safeZones.any { it.bounds.contains(x.toInt(), y.toInt()) }
    }
    
    private fun recordAction(action: ActionType, target: String?, parameters: Map<String, Any>, now: Long) {
        actionHistory.add(ActionRecord(action, target, parameters, now))
        lastActionTime = now
        
        if (actionHistory.size > 100) {
            actionHistory.removeAt(0)
        }
    }
    
    private fun updateScreenHistory(uiMap: UiMap, now: Long) {
        val hash = generateScreenHash(uiMap)
        screenHistory.add(hash)
        
        if (screenHistory.size > 50) {
            screenHistory.removeAt(0)
        }
    }
    
    private fun generateScreenHash(uiMap: UiMap): String {
        return uiMap.currentScreen.elements.joinToString("|") { 
            "${it.bounds.left}-${it.bounds.top}-${it.bounds.right}-${it.bounds.bottom}" 
        }
    }
    
    fun reset() {
        actionHistory.clear()
        screenHistory.clear()
        consecutiveSameActions = 0
        lastActionType = null
        screenFreezeStartTime = 0L
        lastScreenHash = null
        _safetyStatus.value = SafetyStatus.SAFE
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val violations: List<SafetyViolation>
)

data class SafetyViolation(
    val message: String,
    val severity: SafetySeverity
)

data class ActionRecord(
    val action: ActionType,
    val target: String?,
    val parameters: Map<String, Any>,
    val timestamp: Long
)

enum class SafetyStatus {
    SAFE,
    UNSAFE,
    RECOVERING
}
