package com.androidaiagent.core.frame

class FrameSyncBarrier {
    private var latestFrameId: Long = 0L
    private var lastConsumedFrameId: Long = 0L

    fun markProduced(frameId: Long) {
        latestFrameId = frameId
    }

    fun canConsume(frameId: Long): Boolean {
        return frameId > lastConsumedFrameId && frameId <= latestFrameId
    }

    fun markConsumed(frameId: Long) {
        if (frameId > lastConsumedFrameId) {
            lastConsumedFrameId = frameId
        }
    }

    fun reset() {
        latestFrameId = 0L
        lastConsumedFrameId = 0L
    }
}
