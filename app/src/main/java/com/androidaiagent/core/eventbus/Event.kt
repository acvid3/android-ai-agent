package com.androidaiagent.core.eventbus

sealed class Event {
    data class ScreenCaptured(val bitmap: android.graphics.Bitmap, val timestamp: Long) : Event()
    data class AccessibilityEvent(val eventType: String, val packageName: String) : Event()
    data class RouteDetected(val routeName: String, val confidence: Float) : Event()
    data class RouteMatched(val routeName: String, val confidence: Float) : Event()
    data class RouteUnknown(val reason: String) : Event()
    data class ActionExecuted(val action: String, val target: String?, val success: Boolean) : Event()
    data class RouteTransition(val from: String?, val to: String) : Event()
    data class TaskStarted(val taskId: String, val description: String) : Event()
    data class TaskCompleted(val taskId: String, val success: Boolean) : Event()
    data class SafetyViolation(val violation: String, val severity: SafetySeverity) : Event()
    data class RecoveryTriggered(val recoveryType: String, val reason: String) : Event()
    data class AIRequestSent(val requestId: String, val context: String) : Event()
    data class AIResponseReceived(val requestId: String, val response: String) : Event()
    data class StateChanged(val stateName: String, val value: String) : Event()
    data class ErrorOccurred(val error: String, val context: String) : Event()
    data class ScreenStateChanged(val changed: Boolean) : Event()
    data class RecordingStarted(val sessionId: String) : Event()
    data class RecordingStopped(val sessionId: String, val actions: Int) : Event()
    data class RecordingActionRecorded(val action: String, val target: String?) : Event()
}

enum class SafetySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
