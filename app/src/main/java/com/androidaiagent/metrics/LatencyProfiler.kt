package com.androidaiagent.metrics

data class LatencySnapshot(
    val frameProcessingMs: Long = 0L,
    val ocrMs: Long = 0L,
    val aiMs: Long = 0L,
    val actionMs: Long = 0L,
    val routeMatchingMs: Long = 0L
)

class LatencyProfiler {
    private var frameStart: Long = 0L
    private var ocrStart: Long = 0L
    private var aiStart: Long = 0L
    private var actionStart: Long = 0L
    private var routeStart: Long = 0L

    fun markFrameStart() { frameStart = System.currentTimeMillis() }
    fun markOcrStart() { ocrStart = System.currentTimeMillis() }
    fun markAiStart() { aiStart = System.currentTimeMillis() }
    fun markActionStart() { actionStart = System.currentTimeMillis() }
    fun markRouteStart() { routeStart = System.currentTimeMillis() }

    fun snapshot(): LatencySnapshot {
        val now = System.currentTimeMillis()
        return LatencySnapshot(
            frameProcessingMs = if (frameStart == 0L) 0L else now - frameStart,
            ocrMs = if (ocrStart == 0L) 0L else now - ocrStart,
            aiMs = if (aiStart == 0L) 0L else now - aiStart,
            actionMs = if (actionStart == 0L) 0L else now - actionStart,
            routeMatchingMs = if (routeStart == 0L) 0L else now - routeStart
        )
    }
}
