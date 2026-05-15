package com.androidaiagent.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RuntimePhase {
    PERCEPTION,
    ROUTING,
    AI,
    EXECUTION,
    RECOVERY
}

enum class FrameDropPolicy {
    LATEST_ONLY,
    LATEST_STABLE_ONLY
}

class RuntimeScheduler(
    private val perceptionIntervalMs: Long = 500L,
    private val routingIntervalMs: Long = 250L,
    private val aiIntervalMs: Long = 1000L,
    private val executionIntervalMs: Long = 250L,
    private val recoveryIntervalMs: Long = 1000L,
    private val frameDropPolicy: FrameDropPolicy = FrameDropPolicy.LATEST_STABLE_ONLY
) {
    private val lastPhaseTick = mutableMapOf<RuntimePhase, Long>()
    private val _lastProcessedFrameId = MutableStateFlow(0L)
    val lastProcessedFrameId: StateFlow<Long> = _lastProcessedFrameId.asStateFlow()

    fun shouldRunPhase(phase: RuntimePhase, now: Long = System.currentTimeMillis()): Boolean {
        val last = lastPhaseTick[phase] ?: return true
        val interval = when (phase) {
            RuntimePhase.PERCEPTION -> perceptionIntervalMs
            RuntimePhase.ROUTING -> routingIntervalMs
            RuntimePhase.AI -> aiIntervalMs
            RuntimePhase.EXECUTION -> executionIntervalMs
            RuntimePhase.RECOVERY -> recoveryIntervalMs
        }
        return now - last >= interval
    }

    fun markPhase(phase: RuntimePhase, now: Long = System.currentTimeMillis()) {
        lastPhaseTick[phase] = now
    }

    fun shouldProcessFrame(frameId: Long, screenStable: Boolean): Boolean {
        return when (frameDropPolicy) {
            FrameDropPolicy.LATEST_ONLY -> frameId > _lastProcessedFrameId.value
            FrameDropPolicy.LATEST_STABLE_ONLY -> screenStable && frameId > _lastProcessedFrameId.value
        }
    }

    fun markProcessedFrame(frameId: Long) {
        if (frameId > _lastProcessedFrameId.value) {
            _lastProcessedFrameId.value = frameId
        }
    }

    fun reset() {
        lastPhaseTick.clear()
        _lastProcessedFrameId.value = 0L
    }
}
