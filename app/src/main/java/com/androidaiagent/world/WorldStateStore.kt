package com.androidaiagent.world

import android.graphics.Bitmap
import com.androidaiagent.core.AIStatus
import com.androidaiagent.metrics.LatencySnapshot
import com.androidaiagent.runtime.HealthStatus
import com.androidaiagent.runtime.RuntimeMode
import com.androidaiagent.runtime.RuntimeState
import com.androidaiagent.state.AppState
import com.androidaiagent.core.SafetyStatus
import com.androidaiagent.ui.model.UiMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorldStateStore {
    private val _snapshot = MutableStateFlow(WorldStateSnapshot())
    val snapshot: StateFlow<WorldStateSnapshot> = _snapshot.asStateFlow()

    fun update(transform: (WorldStateSnapshot) -> WorldStateSnapshot) {
        _snapshot.value = transform(_snapshot.value)
    }

    fun updateRuntimeState(runtimeState: RuntimeState) {
        update { it.copy(runtimeState = runtimeState, timestamp = System.currentTimeMillis()) }
    }

    fun updateRuntimeMode(runtimeMode: RuntimeMode) {
        update { it.copy(runtimeMode = runtimeMode, timestamp = System.currentTimeMillis()) }
    }

    fun updateFrame(
        frameId: Long,
        timestamp: Long,
        screenshot: Bitmap? = null,
        uiMap: UiMap? = null,
        appState: AppState? = null,
        screenStable: Boolean? = null
    ) {
        update {
            it.copy(
                frameId = frameId,
                timestamp = timestamp,
                screenshot = screenshot ?: it.screenshot,
                uiMap = uiMap ?: it.uiMap,
                currentAppState = appState ?: it.currentAppState,
                screenStable = screenStable ?: it.screenStable
            )
        }
    }

    fun updateRoute(route: String?, confidence: Float) {
        update {
            it.copy(
                currentRoute = route,
                routeConfidence = confidence,
                confidenceMap = it.confidenceMap + ("route" to confidence),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateTask(task: String?) {
        update { it.copy(currentTask = task, timestamp = System.currentTimeMillis()) }
    }

    fun updateModal(modal: String?, blocking: Boolean = false) {
        update {
            it.copy(
                currentModal = modal,
                safetyStatus = if (blocking) SafetyStatus.UNSAFE else it.safetyStatus,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateAction(action: String?, state: ExecutionStatus? = null) {
        update {
            it.copy(
                currentAction = action,
                currentActionState = state?.name ?: it.currentActionState,
                executionStatus = state ?: it.executionStatus,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateAiStatus(status: AIStatus) {
        update { it.copy(aiStatus = status, timestamp = System.currentTimeMillis()) }
    }

    fun updateHealth(status: HealthStatus, lastError: String? = null) {
        update {
            it.copy(
                healthStatus = status,
                lastError = lastError ?: it.lastError,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateSafety(status: SafetyStatus) {
        update { it.copy(safetyStatus = status, timestamp = System.currentTimeMillis()) }
    }

    fun updateLatency(snapshot: LatencySnapshot) {
        update { it.copy(latency = snapshot, timestamp = System.currentTimeMillis()) }
    }

    fun updateQueue(queueSize: Int) {
        update { it.copy(queueSize = queueSize, timestamp = System.currentTimeMillis()) }
    }

    fun updateConfidence(key: String, value: Float) {
        update {
            it.copy(
                confidenceMap = it.confidenceMap + (key to value),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updatePhase(phase: String?) {
        update { it.copy(currentPhase = phase, timestamp = System.currentTimeMillis()) }
    }

    fun updateExecutionOwner(owner: String?) {
        update { it.copy(executionAuthorityOwner = owner, timestamp = System.currentTimeMillis()) }
    }

    fun updateTransition(transition: String?) {
        update { it.copy(currentTransition = transition, timestamp = System.currentTimeMillis()) }
    }

    fun clear() {
        _snapshot.value = WorldStateSnapshot()
    }
}

data class WorldStateSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val frameId: Long = 0L,
    val screenshot: Bitmap? = null,
    val uiMap: UiMap? = null,
    val currentRoute: String? = null,
    val routeConfidence: Float = 0f,
    val currentTask: String? = null,
    val currentModal: String? = null,
    val currentAction: String? = null,
    val currentActionState: String? = null,
    val currentAppState: AppState = AppState.IDLE,
    val currentPhase: String? = null,
    val executionAuthorityOwner: String? = null,
    val currentTransition: String? = null,
    val confidenceMap: Map<String, Float> = emptyMap(),
    val runtimeState: RuntimeState = RuntimeState.STOPPED,
    val runtimeMode: RuntimeMode = RuntimeMode.ASSISTED,
    val aiStatus: AIStatus = AIStatus.IDLE,
    val safetyStatus: SafetyStatus = SafetyStatus.SAFE,
    val healthStatus: HealthStatus = HealthStatus.HEALTHY,
    val executionStatus: ExecutionStatus = ExecutionStatus.IDLE,
    val queueSize: Int = 0,
    val latency: LatencySnapshot = LatencySnapshot(),
    val screenStable: Boolean = true,
    val lastError: String? = null
)

enum class ExecutionStatus {
    IDLE,
    VALIDATING,
    EXECUTING,
    AWAITING_RESULT,
    CONFIRMED,
    FAILED,
    RECOVERED
}
