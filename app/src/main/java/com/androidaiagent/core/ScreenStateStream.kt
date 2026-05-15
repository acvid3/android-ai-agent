package com.androidaiagent.core

import android.graphics.Bitmap
import com.androidaiagent.ui.model.UiMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

class ScreenStateStream {
    private val screenshotChannel = Channel<ScreenshotFrame>(capacity = Channel.UNLIMITED)
    private val uiMapChannel = Channel<UiMap>(capacity = Channel.UNLIMITED)
    private val _screenshotSharedFlow = MutableSharedFlow<ScreenshotFrame>(replay = 1, extraBufferCapacity = 4)
    private val _uiMapSharedFlow = MutableSharedFlow<UiMap>(replay = 1, extraBufferCapacity = 4)
    
    private val _currentScreenshot = MutableStateFlow<Bitmap?>(null)
    val currentScreenshot: StateFlow<Bitmap?> = _currentScreenshot.asStateFlow()
    
    private val _currentUiMap = MutableStateFlow<UiMap?>(null)
    val currentUiMap: StateFlow<UiMap?> = _currentUiMap.asStateFlow()
    
    private val _screenChangeDetected = MutableStateFlow(false)
    val screenChangeDetected: StateFlow<Boolean> = _screenChangeDetected.asStateFlow()
    
    private var lastScreenHash: String? = null
    
    val screenshotFlow: Flow<ScreenshotFrame> = screenshotChannel.receiveAsFlow()
    val uiMapFlow: Flow<UiMap> = uiMapChannel.receiveAsFlow()
    val screenshotSharedFlow: SharedFlow<ScreenshotFrame> = _screenshotSharedFlow.asSharedFlow()
    val uiMapSharedFlow: SharedFlow<UiMap> = _uiMapSharedFlow.asSharedFlow()
    
    suspend fun publishScreenshot(bitmap: Bitmap, timestamp: Long = System.currentTimeMillis(), frameId: Long = 0L) {
        val frame = ScreenshotFrame(bitmap, timestamp, frameId)
        screenshotChannel.send(frame)
        _screenshotSharedFlow.emit(frame)
        _currentScreenshot.value = bitmap
        
        val currentHash = generateScreenHash(bitmap)
        _screenChangeDetected.value = currentHash != lastScreenHash
        lastScreenHash = currentHash
    }
    
    suspend fun publishUiMap(uiMap: UiMap) {
        uiMapChannel.send(uiMap)
        _uiMapSharedFlow.emit(uiMap)
        _currentUiMap.value = uiMap
    }
    
    private fun generateScreenHash(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var hash = 17L
        for (i in pixels.indices step 100) {
            hash = hash * 31 + pixels[i]
        }
        return hash.toString()
    }
    
    fun clear() {
        _currentScreenshot.value = null
        _currentUiMap.value = null
        _screenChangeDetected.value = false
        lastScreenHash = null
        _screenshotSharedFlow.resetReplayCache()
        _uiMapSharedFlow.resetReplayCache()
    }
}

data class ScreenshotFrame(
    val bitmap: Bitmap,
    val timestamp: Long,
    val frameId: Long = 0L
)
