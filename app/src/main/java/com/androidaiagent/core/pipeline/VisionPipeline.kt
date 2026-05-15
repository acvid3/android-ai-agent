package com.androidaiagent.core.pipeline

import android.graphics.Bitmap
import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.core.eventbus.EventBus
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VisionPipeline(
    private val ocrStage: OCRStage,
    private val accessibilityStage: AccessibilityParseStage,
    private val iconDetectionStage: IconDetectionStage,
    private val templateMatchingStage: TemplateMatchingStage,
    private val uiMapGenerationStage: UiMapGenerationStage,
    private val routeMatchingStage: RouteMatchingStage,
    private val eventBus: EventBus = GlobalEventBus
) {
    suspend fun processScreenshot(bitmap: Bitmap): UiMap {
        val timestamp = System.currentTimeMillis()
        eventBus.tryPublish(Event.ScreenCaptured(bitmap, timestamp))
        
        val ocrResult = ocrStage.process(bitmap)
        val accessibilityResult = accessibilityStage.process(bitmap)
        val icons = iconDetectionStage.process(bitmap to accessibilityResult)
        val templates = templateMatchingStage.process(bitmap to ocrResult)
        val screenshotHash = generateScreenshotHash(bitmap)
        val uiMap = uiMapGenerationStage.process(
            UiMapInput(bitmap, ocrResult, accessibilityResult, icons, templates, screenshotHash)
        )
        val routeMatch = routeMatchingStage.process(uiMap)

        return routeMatch.routeName?.let { uiMap.withRoute(it, routeMatch.confidence) } ?: uiMap
    }
    
    fun createProcessingFlow(screenshotFlow: Flow<Bitmap>): Flow<UiMap> = flow {
        screenshotFlow.collect { bitmap ->
            emit(processScreenshot(bitmap))
        }
    }
}

data class UiMapInput(
    val bitmap: Bitmap,
    val ocrResult: OCRResult,
    val accessibilityResult: AccessibilityResult,
    val icons: List<com.androidaiagent.ui.model.DetectedIcon>,
    val templates: List<TemplateMatch>,
    val screenshotHash: String
)

data class OCRResult(val textRegions: List<TextRegion>)
data class AccessibilityResult(val nodes: List<AccessibilityNode>)
data class TextRegion(val text: String, val bounds: android.graphics.Rect, val confidence: Float = 1f)
data class AccessibilityNode(val node: android.view.accessibility.AccessibilityNodeInfo)
data class TemplateMatch(val templateName: String, val bounds: android.graphics.Rect, val confidence: Float)

private fun generateScreenshotHash(bitmap: Bitmap): String {
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
