package com.androidaiagent.metrics

import com.androidaiagent.runtime.HealthStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HealthSnapshot(
    val status: HealthStatus = HealthStatus.HEALTHY,
    val frameStallMs: Long = 0L,
    val ocrLagMs: Long = 0L,
    val aiLagMs: Long = 0L,
    val overlayLagMs: Long = 0L,
    val queueSize: Int = 0,
    val consecutiveStalls: Int = 0,
    val lastError: String? = null
)

class HealthMonitor(
    private val frameStallThresholdMs: Long = 10_000L,
    private val ocrLagThresholdMs: Long = 15_000L,
    private val aiLagThresholdMs: Long = 20_000L,
    private val overlayLagThresholdMs: Long = 10_000L,
    private val queueGrowthThreshold: Int = 24
) {
    private val _snapshot = MutableStateFlow(HealthSnapshot())
    val snapshot: StateFlow<HealthSnapshot> = _snapshot.asStateFlow()

    private var lastFrameAt = 0L
    private var lastOcrAt = 0L
    private var lastAiAt = 0L
    private var lastOverlayAt = 0L
    private var consecutiveStalls = 0

    fun reportFrame(now: Long = System.currentTimeMillis()) {
        lastFrameAt = now
    }

    fun reportOcr(now: Long = System.currentTimeMillis()) {
        lastOcrAt = now
    }

    fun reportAi(now: Long = System.currentTimeMillis()) {
        lastAiAt = now
    }

    fun reportOverlay(now: Long = System.currentTimeMillis()) {
        lastOverlayAt = now
    }

    fun evaluate(queueSize: Int = 0, now: Long = System.currentTimeMillis()): HealthSnapshot {
        val frameStallMs = elapsed(now, lastFrameAt)
        val ocrLagMs = elapsed(now, lastOcrAt)
        val aiLagMs = elapsed(now, lastAiAt)
        val overlayLagMs = elapsed(now, lastOverlayAt)

        val unhealthy = frameStallMs > frameStallThresholdMs ||
            ocrLagMs > ocrLagThresholdMs ||
            aiLagMs > aiLagThresholdMs ||
            overlayLagMs > overlayLagThresholdMs ||
            queueSize > queueGrowthThreshold

        val status = when {
            unhealthy -> HealthStatus.UNHEALTHY
            frameStallMs > frameStallThresholdMs / 2 ||
                ocrLagMs > ocrLagThresholdMs / 2 ||
                aiLagMs > aiLagThresholdMs / 2 ||
                queueSize > queueGrowthThreshold / 2 -> HealthStatus.RECOVERING
            else -> HealthStatus.HEALTHY
        }

        consecutiveStalls = if (unhealthy) consecutiveStalls + 1 else 0

        val snapshot = HealthSnapshot(
            status = status,
            frameStallMs = frameStallMs,
            ocrLagMs = ocrLagMs,
            aiLagMs = aiLagMs,
            overlayLagMs = overlayLagMs,
            queueSize = queueSize,
            consecutiveStalls = consecutiveStalls
        )
        _snapshot.value = snapshot
        return snapshot
    }

    fun reset() {
        lastFrameAt = 0L
        lastOcrAt = 0L
        lastAiAt = 0L
        lastOverlayAt = 0L
        consecutiveStalls = 0
        _snapshot.value = HealthSnapshot()
    }

    private fun elapsed(now: Long, then: Long): Long = if (then == 0L) Long.MAX_VALUE / 4 else now - then
}
