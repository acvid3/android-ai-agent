package com.androidaiagent.recovery

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.accessibility.AccessibilityService
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.EventBus
import com.androidaiagent.core.eventbus.GlobalEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import android.accessibilityservice.AccessibilityService as AndroidAccessibilityService

class RecoverySystem(
    private val accessibilityService: AccessibilityService,
    private val scope: CoroutineScope,
    private val eventBus: EventBus = GlobalEventBus
) {
    private val recoveryStrategies = mutableMapOf<RecoveryType, RecoveryStrategy>()
    
    init {
        registerDefaultStrategies()
    }
    
    private fun registerDefaultStrategies() {
        recoveryStrategies[RecoveryType.STUCK_UI] = StuckUIRecoveryStrategy()
        recoveryStrategies[RecoveryType.LOST_NAVIGATION] = LostNavigationRecoveryStrategy()
        recoveryStrategies[RecoveryType.APP_CRASH] = AppCrashRecoveryStrategy()
        recoveryStrategies[RecoveryType.UNKNOWN_POPUP] = UnknownPopupRecoveryStrategy()
        recoveryStrategies[RecoveryType.INFINITE_LOOP] = InfiniteLoopRecoveryStrategy()
        recoveryStrategies[RecoveryType.SCREEN_FREEZE] = ScreenFreezeRecoveryStrategy()
        recoveryStrategies[RecoveryType.RETURN_TO_HOME] = ReturnToHomeRecoveryStrategy()
    }
    
    fun registerStrategy(type: RecoveryType, strategy: RecoveryStrategy) {
        recoveryStrategies[type] = strategy
    }

    suspend fun recoverNavigation(reason: String, uiMap: UiMap? = null): Boolean {
        return triggerRecovery(RecoveryType.LOST_NAVIGATION, RecoveryContext(reason, uiMap))
    }

    suspend fun recoverUi(reason: String, uiMap: UiMap? = null): Boolean {
        return triggerRecovery(RecoveryType.STUCK_UI, RecoveryContext(reason, uiMap))
    }

    suspend fun recoverApp(reason: String, uiMap: UiMap? = null): Boolean {
        return triggerRecovery(RecoveryType.APP_CRASH, RecoveryContext(reason, uiMap))
    }

    suspend fun recoverAi(reason: String, uiMap: UiMap? = null): Boolean {
        return triggerRecovery(RecoveryType.INFINITE_LOOP, RecoveryContext(reason, uiMap))
    }

    suspend fun recoverStuckAction(reason: String, uiMap: UiMap? = null): Boolean {
        return triggerRecovery(RecoveryType.INFINITE_LOOP, RecoveryContext(reason, uiMap))
    }

    suspend fun recoverUnknownPopup(reason: String, uiMap: UiMap? = null): Boolean {
        return triggerRecovery(RecoveryType.UNKNOWN_POPUP, RecoveryContext(reason, uiMap))
    }
    
    suspend fun triggerRecovery(type: RecoveryType, context: RecoveryContext): Boolean {
        val strategy = recoveryStrategies[type] ?: return false
        
        eventBus.tryPublish(Event.RecoveryTriggered(type.name, context.reason))
        
        var attempt = 0
        val maxAttempts = 3
        
        while (attempt < maxAttempts) {
            attempt++
            
            val success = strategy.execute(context, accessibilityService)
            
            if (success) {
                eventBus.tryPublish(Event.StateChanged("recovery", "success"))
                return true
            }
            
            delay(1000L * attempt)
        }
        
        eventBus.tryPublish(Event.StateChanged("recovery", "failed"))
        return false
    }
    
    suspend fun detectAndRecover(uiMap: UiMap): RecoveryType? {
        if (isScreenFrozen(uiMap)) {
            recoverUi("Screen freeze detected", uiMap)
            return RecoveryType.SCREEN_FREEZE
        }
        
        if (isUnknownPopup(uiMap)) {
            recoverUnknownPopup("Unknown popup detected", uiMap)
            return RecoveryType.UNKNOWN_POPUP
        }

        if (uiMap.routeConfidence < 0.3f && uiMap.currentScreen.elements.isNotEmpty()) {
            recoverNavigation("Lost navigation detected", uiMap)
            return RecoveryType.LOST_NAVIGATION
        }
        
        return null
    }
    
    private fun isScreenFrozen(uiMap: UiMap): Boolean {
        return uiMap.routeConfidence < 0.3f && uiMap.currentScreen.elements.isEmpty()
    }
    
    private fun isUnknownPopup(uiMap: UiMap): Boolean {
        return uiMap.detectedRoute == "unknown" && uiMap.currentScreen.buttons.size < 3
    }
}

interface RecoveryStrategy {
    suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean
}

data class RecoveryContext(
    val reason: String,
    val uiMap: UiMap? = null,
    val additionalData: Map<String, Any> = emptyMap()
)

class StuckUIRecoveryStrategy : RecoveryStrategy {
    override suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean {
        repeat(3) {
            accessibilityService.performBack()
            kotlinx.coroutines.delay(500)
        }
        return true
    }
}

class LostNavigationRecoveryStrategy : RecoveryStrategy {
    override suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean {
        accessibilityService.performBack()
        kotlinx.coroutines.delay(500)
        accessibilityService.performBack()
        return true
    }
}

class AppCrashRecoveryStrategy : RecoveryStrategy {
    override suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean {
        kotlinx.coroutines.delay(2000)
        return true
    }
}

class UnknownPopupRecoveryStrategy : RecoveryStrategy {
    override suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean {
        accessibilityService.performBack()
        kotlinx.coroutines.delay(500)
        return true
    }
}

class InfiniteLoopRecoveryStrategy : RecoveryStrategy {
    override suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean {
        kotlinx.coroutines.delay(3000)
        accessibilityService.performBack()
        return true
    }
}

class ScreenFreezeRecoveryStrategy : RecoveryStrategy {
    override suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean {
        accessibilityService.performBack()
        kotlinx.coroutines.delay(1000)
        return true
    }
}

class ReturnToHomeRecoveryStrategy : RecoveryStrategy {
    override suspend fun execute(context: RecoveryContext, accessibilityService: AccessibilityService): Boolean {
        accessibilityService.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_HOME)
        kotlinx.coroutines.delay(1000)
        return true
    }
}

enum class RecoveryType {
    STUCK_UI,
    LOST_NAVIGATION,
    APP_CRASH,
    UNKNOWN_POPUP,
    INFINITE_LOOP,
    SCREEN_FREEZE,
    RETURN_TO_HOME
}
