package com.androidaiagent.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedAppState {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute: StateFlow<String?> = _currentRoute.asStateFlow()
    
    private val _currentTask = MutableStateFlow<String?>(null)
    val currentTask: StateFlow<String?> = _currentTask.asStateFlow()
    
    private val _lastAction = MutableStateFlow<String?>(null)
    val lastAction: StateFlow<String?> = _lastAction.asStateFlow()
    
    private val _routeConfidence = MutableStateFlow(0f)
    val routeConfidence: StateFlow<Float> = _routeConfidence.asStateFlow()
    
    private val _aiStatus = MutableStateFlow(AIStatus.IDLE)
    val aiStatus: StateFlow<AIStatus> = _aiStatus.asStateFlow()
    
    private val _safetyStatus = MutableStateFlow(SafetyStatus.SAFE)
    val safetyStatus: StateFlow<SafetyStatus> = _safetyStatus.asStateFlow()
    
    private val _screenFreezeDetected = MutableStateFlow(false)
    val screenFreezeDetected: StateFlow<Boolean> = _screenFreezeDetected.asStateFlow()
    
    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }
    
    fun setCurrentRoute(route: String?, confidence: Float = 0f) {
        _currentRoute.value = route
        _routeConfidence.value = confidence
    }
    
    fun setCurrentTask(task: String?) {
        _currentTask.value = task
    }
    
    fun setLastAction(action: String?) {
        _lastAction.value = action
    }
    
    fun setAIStatus(status: AIStatus) {
        _aiStatus.value = status
    }
    
    fun setSafetyStatus(status: SafetyStatus) {
        _safetyStatus.value = status
    }
    
    fun setScreenFreezeDetected(detected: Boolean) {
        _screenFreezeDetected.value = detected
    }
    
    fun reset() {
        _isRunning.value = false
        _currentRoute.value = null
        _currentTask.value = null
        _lastAction.value = null
        _routeConfidence.value = 0f
        _aiStatus.value = AIStatus.IDLE
        _safetyStatus.value = SafetyStatus.SAFE
        _screenFreezeDetected.value = false
    }
}

enum class AIStatus {
    IDLE,
    PROCESSING,
    WAITING,
    ERROR
}

enum class SafetyStatus {
    SAFE,
    WARNING,
    UNSAFE,
    RECOVERING
}
