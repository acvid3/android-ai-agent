package com.androidaiagent.vision.stabilization

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenStabilizationLayer(
    private val stableFrameThreshold: Int = 3,
    private val transitionWindowMs: Long = 500L,
    private val loadingFrameThreshold: Int = 4
) {
    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable.asStateFlow()

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastHash: String? = null
    private var lastChangeAt = 0L
    private var stableCount = 0
    private var changeCount = 0

    fun registerFrame(bitmap: Bitmap): StabilizationStatus {
        val now = System.currentTimeMillis()
        val hash = bitmap.fastHash()
        val changed = hash != lastHash

        if (changed) {
            changeCount++
            stableCount = 0
            _isTransitioning.value = now - lastChangeAt <= transitionWindowMs
            lastChangeAt = now
            lastHash = hash
        } else {
            stableCount++
            _isTransitioning.value = false
        }

        _isLoading.value = changeCount >= loadingFrameThreshold && stableCount == 0
        _isStable.value = stableCount >= stableFrameThreshold

        return StabilizationStatus(
            isStable = _isStable.value,
            isTransitioning = _isTransitioning.value,
            isLoading = _isLoading.value,
            frameHash = hash,
            stableFrameCount = stableCount
        )
    }

    fun shouldAnalyze(bitmap: Bitmap): Boolean {
        val status = registerFrame(bitmap)
        return status.isStable && !status.isTransitioning && !status.isLoading
    }

    fun reset() {
        lastHash = null
        lastChangeAt = 0L
        stableCount = 0
        changeCount = 0
        _isStable.value = false
        _isTransitioning.value = false
        _isLoading.value = false
    }

    private fun Bitmap.fastHash(): String {
        val width = width
        val height = height
        val sampleWidth = minOf(width, 24)
        val sampleHeight = minOf(height, 24)
        val pixels = IntArray(sampleWidth * sampleHeight)
        getPixels(pixels, 0, sampleWidth, 0, 0, sampleWidth, sampleHeight)
        var hash = 17L
        for (pixel in pixels) {
            hash = hash * 31 + pixel
        }
        return hash.toString()
    }
}

data class StabilizationStatus(
    val isStable: Boolean,
    val isTransitioning: Boolean,
    val isLoading: Boolean,
    val frameHash: String,
    val stableFrameCount: Int
)
