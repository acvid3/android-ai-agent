package com.androidaiagent.core.frame

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class FrameClock {
    private val frameIdGenerator = AtomicLong(0L)
    private val _currentFrame = MutableStateFlow<FrameVersion?>(null)
    val currentFrame: StateFlow<FrameVersion?> = _currentFrame.asStateFlow()

    fun tick(timestamp: Long = System.currentTimeMillis()): FrameVersion {
        val frame = FrameVersion(
            id = frameIdGenerator.incrementAndGet(),
            timestamp = timestamp
        )
        _currentFrame.value = frame
        return frame
    }

    fun currentFrameId(): Long = _currentFrame.value?.id ?: 0L
}

data class FrameVersion(
    val id: Long,
    val timestamp: Long
)
