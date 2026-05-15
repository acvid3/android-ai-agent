package com.androidaiagent.diagnostics

import android.graphics.Bitmap
import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.core.eventbus.EventBus
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DiagnosticsSystem(
    private val eventBus: EventBus = GlobalEventBus
) {
    private val actionLog = mutableListOf<ActionLogEntry>()
    private val screenshotHistory = mutableListOf<ScreenshotEntry>()
    private val routeDebugLog = mutableListOf<RouteDebugEntry>()
    private val ocrDebugLog = mutableListOf<OCRDebugEntry>()
    private val runtimeTraceLog = mutableListOf<RuntimeTraceEntry>()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _logSize = MutableStateFlow(0)
    val logSize: StateFlow<Int> = _logSize.asStateFlow()
    
    private var logDirectory: File? = null
    
    fun initialize(directory: File) {
        logDirectory = directory
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }
    
    fun startRecording() {
        _isRecording.value = true
    }
    
    fun stopRecording() {
        _isRecording.value = false
    }
    
    fun logAction(action: String, target: String?, parameters: Map<String, Any>, success: Boolean) {
        if (!_isRecording.value) return
        
        val entry = ActionLogEntry(
            action = action,
            target = target,
            parameters = parameters,
            success = success,
            timestamp = System.currentTimeMillis()
        )
        
        actionLog.add(entry)
        _logSize.value = getTotalLogSize()
        
        if (actionLog.size > 1000) {
            actionLog.removeAt(0)
        }
    }
    
    fun logScreenshot(bitmap: Bitmap, uiMap: UiMap) {
        if (!_isRecording.value) return
        
        val entry = ScreenshotEntry(
            bitmap = bitmap,
            uiMap = uiMap,
            timestamp = System.currentTimeMillis()
        )
        
        screenshotHistory.add(entry)
        
        if (screenshotHistory.size > 100) {
            screenshotHistory.removeAt(0)
        }
    }
    
    fun logRouteDetection(route: String, confidence: Float, matchedElements: List<String>) {
        if (!_isRecording.value) return
        
        val entry = RouteDebugEntry(
            route = route,
            confidence = confidence,
            matchedElements = matchedElements,
            timestamp = System.currentTimeMillis()
        )
        
        routeDebugLog.add(entry)
        
        if (routeDebugLog.size > 500) {
            routeDebugLog.removeAt(0)
        }
    }
    
    fun logOCRResult(text: String, confidence: Float, bounds: android.graphics.Rect) {
        if (!_isRecording.value) return
        
        val entry = OCRDebugEntry(
            text = text,
            confidence = confidence,
            bounds = bounds,
            timestamp = System.currentTimeMillis()
        )
        
        ocrDebugLog.add(entry)
        
        if (ocrDebugLog.size > 500) {
            ocrDebugLog.removeAt(0)
        }
    }

    fun logRuntimeTrace(
        frameId: Long,
        phase: String,
        route: String?,
        action: String?,
        result: String?,
        latencyMs: Long
    ) {
        if (!_isRecording.value) return

        runtimeTraceLog.add(
            RuntimeTraceEntry(
                frameId = frameId,
                phase = phase,
                route = route,
                action = action,
                result = result,
                latencyMs = latencyMs,
                timestamp = System.currentTimeMillis()
            )
        )

        if (runtimeTraceLog.size > 500) {
            runtimeTraceLog.removeAt(0)
        }
    }
    
    fun getActionLog(): List<ActionLogEntry> {
        return actionLog.toList()
    }
    
    fun getScreenshotHistory(): List<ScreenshotEntry> {
        return screenshotHistory.toList()
    }
    
    fun getRouteDebugLog(): List<RouteDebugEntry> {
        return routeDebugLog.toList()
    }
    
    fun getOCRDebugLog(): List<OCRDebugEntry> {
        return ocrDebugLog.toList()
    }

    fun getRuntimeTraceLog(): List<RuntimeTraceEntry> {
        return runtimeTraceLog.toList()
    }
    
    fun exportLogs(): File? {
        val dir = logDirectory ?: return null
        
        val timestamp = System.currentTimeMillis()
        val logFile = File(dir, "diagnostics_$timestamp.json")
        
        val exportData = DiagnosticsExport(
            actionLog = actionLog,
            routeDebugLog = routeDebugLog,
            ocrDebugLog = ocrDebugLog,
            runtimeTraceLog = runtimeTraceLog,
            exportTimestamp = timestamp
        )
        
        val json = JSONObject().apply {
            put("exportTimestamp", exportData.exportTimestamp)
            put("actionLog", JSONArray().apply {
                exportData.actionLog.forEach { entry ->
                    put(JSONObject().apply {
                        put("action", entry.action)
                        put("target", entry.target)
                        put("success", entry.success)
                        put("timestamp", entry.timestamp)
                    })
                }
            })
            put("routeDebugLog", JSONArray().apply {
                exportData.routeDebugLog.forEach { entry ->
                    put(JSONObject().apply {
                        put("route", entry.route)
                        put("confidence", entry.confidence)
                        put("matchedElements", JSONArray(entry.matchedElements))
                        put("timestamp", entry.timestamp)
                    })
                }
            })
            put("ocrDebugLog", JSONArray().apply {
                exportData.ocrDebugLog.forEach { entry ->
                    put(JSONObject().apply {
                        put("text", entry.text)
                        put("confidence", entry.confidence)
                        put("bounds", JSONObject().apply {
                            put("left", entry.bounds.left)
                            put("top", entry.bounds.top)
                            put("right", entry.bounds.right)
                            put("bottom", entry.bounds.bottom)
                        })
                        put("timestamp", entry.timestamp)
                    })
                }
            })
            put("runtimeTraceLog", JSONArray().apply {
                exportData.runtimeTraceLog.forEach { entry ->
                    put(JSONObject().apply {
                        put("frameId", entry.frameId)
                        put("phase", entry.phase)
                        put("route", entry.route)
                        put("action", entry.action)
                        put("result", entry.result)
                        put("latencyMs", entry.latencyMs)
                        put("timestamp", entry.timestamp)
                    })
                }
            })
        }
        logFile.writeText(json.toString(2))
        return logFile
    }
    
    fun replayActions(fromIndex: Int = 0) {
        val actionsToReplay = actionLog.drop(fromIndex)
        
        actionsToReplay.forEach { entry ->
            eventBus.tryPublish(Event.ActionExecuted(entry.action, entry.target, entry.success))
        }
    }
    
    fun clearLogs() {
        actionLog.clear()
        screenshotHistory.clear()
        routeDebugLog.clear()
        ocrDebugLog.clear()
        _logSize.value = 0
    }
    
    private fun getTotalLogSize(): Int {
        return actionLog.size + screenshotHistory.size + routeDebugLog.size + ocrDebugLog.size + runtimeTraceLog.size
    }
}

data class ActionLogEntry(
    val action: String,
    val target: String?,
    val parameters: Map<String, Any>,
    val success: Boolean,
    val timestamp: Long
)

data class ScreenshotEntry(
    val bitmap: Bitmap,
    val uiMap: UiMap,
    val timestamp: Long
)

data class RouteDebugEntry(
    val route: String,
    val confidence: Float,
    val matchedElements: List<String>,
    val timestamp: Long
)

data class OCRDebugEntry(
    val text: String,
    val confidence: Float,
    val bounds: android.graphics.Rect,
    val timestamp: Long
)

data class RuntimeTraceEntry(
    val frameId: Long,
    val phase: String,
    val route: String?,
    val action: String?,
    val result: String?,
    val latencyMs: Long,
    val timestamp: Long
)

data class DiagnosticsExport(
    val actionLog: List<ActionLogEntry>,
    val routeDebugLog: List<RouteDebugEntry>,
    val ocrDebugLog: List<OCRDebugEntry>,
    val runtimeTraceLog: List<RuntimeTraceEntry>,
    val exportTimestamp: Long
)
