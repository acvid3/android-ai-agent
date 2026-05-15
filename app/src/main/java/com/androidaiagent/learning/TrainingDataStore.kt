package com.androidaiagent.learning

import android.graphics.Bitmap
import com.androidaiagent.routing.RouteDefinition
import com.androidaiagent.ui.model.UiMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.max

class TrainingDataStore(private val baseDir: File) {
    private val recordingsDir = File(baseDir, "recordings")
    private val routeSamplesDir = File(baseDir, "route_samples")
    private val uiSnapshotsDir = File(baseDir, "ui_snapshots")
    private val screenshotStoragePolicy = ScreenshotStoragePolicy()

    init {
        recordingsDir.mkdirs()
        routeSamplesDir.mkdirs()
        uiSnapshotsDir.mkdirs()
    }

    fun saveUnknownScreen(uiMap: UiMap, reason: String) {
        saveSnapshot("unknown_${uiMap.timestamp}.json", uiMap, reason, "unknown")
    }

    fun saveFailedRoute(route: String?, uiMap: UiMap, reason: String) {
        val fileName = "failed_${route ?: "route"}_${uiMap.timestamp}.json"
        saveSnapshot(fileName, uiMap, reason, route)
    }

    fun saveAmbiguousState(uiMap: UiMap, reason: String) {
        saveSnapshot("ambiguous_${uiMap.timestamp}.json", uiMap, reason, uiMap.detectedRoute)
    }

    fun saveRecordingMetadata(
        routeDefinitions: List<RouteDefinition>,
        sessionId: String,
        notes: String = ""
    ) {
        val file = File(recordingsDir, "$sessionId.json")
        val json = JSONObject().apply {
            put("sessionId", sessionId)
            put("notes", notes)
            put("routes", JSONArray().apply {
                routeDefinitions.forEach { route ->
                    put(JSONObject().apply {
                        put("name", route.name)
                        put("description", route.description)
                        put("purpose", route.purpose)
                        put("expectedButtons", JSONArray(route.expectedButtons))
                        put("expectedText", JSONArray(route.expectedText))
                        put("availableActions", JSONArray(route.availableActions))
                    })
                }
            })
        }
        file.writeText(json.toString(2))
    }

    fun saveSnapshot(name: String, uiMap: UiMap, reason: String, route: String?) {
        if (!screenshotStoragePolicy.shouldPersist(reason)) {
            pruneSnapshots()
            return
        }
        val file = File(uiSnapshotsDir, name)
        val json = JSONObject().apply {
            put("reason", reason)
            put("route", route)
            put("timestamp", uiMap.timestamp)
            put("packageName", uiMap.currentScreen.packageName)
            put("activityName", uiMap.currentScreen.activityName)
            put("elements", JSONArray().apply {
                uiMap.currentScreen.elements.forEach { element ->
                    put(JSONObject().apply {
                        put("type", element::class.simpleName)
                        put("left", element.bounds.left)
                        put("top", element.bounds.top)
                        put("right", element.bounds.right)
                        put("bottom", element.bounds.bottom)
                        put("confidence", element.confidence)
                    })
                }
            })
        }
        file.writeText(json.toString(2))
        pruneSnapshots()
    }

    fun listSnapshots(): List<File> = uiSnapshotsDir.listFiles()?.toList().orEmpty()
    fun listRecordings(): List<File> = recordingsDir.listFiles()?.toList().orEmpty()
    fun listRouteSamples(): List<File> = routeSamplesDir.listFiles()?.toList().orEmpty()

    private fun pruneSnapshots(maxFiles: Int = 50) {
        val files = uiSnapshotsDir.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
        if (files.size <= maxFiles) return
        files.take(max(0, files.size - maxFiles)).forEach { it.delete() }
    }
}

class ScreenshotStoragePolicy(
    private val failureOnly: Boolean = true
) {
    fun shouldPersist(reason: String): Boolean {
        return if (!failureOnly) true else reason.contains("fail", ignoreCase = true) ||
            reason.contains("error", ignoreCase = true) ||
            reason.contains("unknown", ignoreCase = true) ||
            reason.contains("modal", ignoreCase = true)
    }
}
