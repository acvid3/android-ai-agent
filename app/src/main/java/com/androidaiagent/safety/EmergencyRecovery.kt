package com.androidaiagent.safety

import com.androidaiagent.accessibility.AccessibilityService
import com.androidaiagent.core.eventbus.EventBus
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import com.androidaiagent.core.eventbus.SafetySeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EmergencyRecovery(
    private val accessibilityService: AccessibilityService,
    private val scope: CoroutineScope,
    private val eventBus: EventBus = GlobalEventBus
) {
    private var isRecovering = false
    
    fun triggerRecovery(recoveryType: RecoveryType, reason: String) {
        if (isRecovering) return
        
        isRecovering = true
        eventBus.tryPublish(Event.RecoveryTriggered(recoveryType.name, reason))
        
        scope.launch {
            try {
                when (recoveryType) {
                    RecoveryType.STUCK_UI -> recoverFromStuckUI()
                    RecoveryType.LOST_NAVIGATION -> recoverFromLostNavigation()
                    RecoveryType.APP_CRASH -> recoverFromAppCrash()
                    RecoveryType.UNKNOWN_POPUP -> recoverFromUnknownPopup()
                    RecoveryType.INFINITE_LOOP -> recoverFromInfiniteLoop()
                    RecoveryType.SCREEN_FREEZE -> recoverFromScreenFreeze()
                }
            } catch (e: Exception) {
                eventBus.tryPublish(Event.ErrorOccurred(e.message ?: "Recovery failed", "EmergencyRecovery"))
            } finally {
                isRecovering = false
            }
        }
    }
    
    private suspend fun recoverFromStuckUI() {
        repeat(3) {
            accessibilityService.performBack()
            delay(1000)
        }
    }
    
    private suspend fun recoverFromLostNavigation() {
        accessibilityService.performBack()
        delay(500)
        accessibilityService.performBack()
        delay(500)
    }
    
    private suspend fun recoverFromAppCrash() {
        delay(2000)
    }
    
    private suspend fun recoverFromUnknownPopup() {
        accessibilityService.performBack()
        delay(500)
    }
    
    private suspend fun recoverFromInfiniteLoop() {
        delay(3000)
    }
    
    private suspend fun recoverFromScreenFreeze() {
        accessibilityService.performBack()
        delay(1000)
    }
}

enum class RecoveryType {
    STUCK_UI,
    LOST_NAVIGATION,
    APP_CRASH,
    UNKNOWN_POPUP,
    INFINITE_LOOP,
    SCREEN_FREEZE
}
