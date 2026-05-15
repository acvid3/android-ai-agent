package com.androidaiagent.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ResourceManager(
    private val baseAnalysisIntervalMs: Long = 1000L,
    private val lowPowerAnalysisIntervalMs: Long = 2500L
) {
    private val _analysisIntervalMs = MutableStateFlow(baseAnalysisIntervalMs)
    val analysisIntervalMs: StateFlow<Long> = _analysisIntervalMs.asStateFlow()

    private val _isLowPowerMode = MutableStateFlow(false)
    val isLowPowerMode: StateFlow<Boolean> = _isLowPowerMode.asStateFlow()

    private val _isIdleMode = MutableStateFlow(false)
    val isIdleMode: StateFlow<Boolean> = _isIdleMode.asStateFlow()

    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()

    private var lastAnalysisAt = 0L

    fun setLowPowerMode(enabled: Boolean) {
        _isLowPowerMode.value = enabled
        recalculateAnalysisInterval()
    }

    fun setIdleMode(enabled: Boolean) {
        _isIdleMode.value = enabled
        recalculateAnalysisInterval()
    }

    fun setThermalState(state: ThermalState) {
        _thermalState.value = state
        recalculateAnalysisInterval()
    }

    fun shouldThrottleCapture(now: Long = System.currentTimeMillis()): Boolean {
        val interval = _analysisIntervalMs.value
        return now - lastAnalysisAt < interval
    }

    fun markAnalysis(now: Long = System.currentTimeMillis()) {
        lastAnalysisAt = now
    }

    fun cleanupSchedulerTick(now: Long = System.currentTimeMillis()): Boolean {
        return now - lastAnalysisAt > 5 * 60 * 1000L
    }

    private fun recalculateAnalysisInterval() {
        val multiplier = when {
            _thermalState.value == ThermalState.CRITICAL -> 4.0
            _thermalState.value == ThermalState.WARM -> 2.0
            _isLowPowerMode.value -> 2.5
            _isIdleMode.value -> 1.8
            else -> 1.0
        }

        val base = if (_isLowPowerMode.value) lowPowerAnalysisIntervalMs else baseAnalysisIntervalMs
        _analysisIntervalMs.value = (base * multiplier).toLong().coerceAtLeast(250L)
    }
}

enum class ThermalState {
    NORMAL,
    WARM,
    CRITICAL
}
