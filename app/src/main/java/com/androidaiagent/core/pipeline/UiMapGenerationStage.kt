package com.androidaiagent.core.pipeline

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.ui.model.UiScreen
import com.androidaiagent.ui.model.DetectedButton
import com.androidaiagent.ui.model.DetectedText
import com.androidaiagent.ui.model.DetectedTextField
import com.androidaiagent.ui.model.InteractionZone
import android.graphics.Rect

class UiMapGenerationStage : PipelineStage<UiMapInput, UiMap>() {
    override suspend fun process(input: UiMapInput): UiMap {
        val elements = mutableListOf<com.androidaiagent.ui.model.UiElement>()
        
        input.ocrResult.textRegions.forEach { region ->
            elements.add(DetectedText(region.bounds, 0.8f, region.text))
        }
        
        input.accessibilityResult.nodes.forEach { node ->
            if (node.node.isClickable) {
                elements.add(DetectedButton(
                    bounds = Rect(node.node.boundsInScreen),
                    confidence = 0.9f,
                    text = node.node.text?.toString(),
                    contentDescription = node.node.contentDescription?.toString(),
                    resourceId = node.node.viewIdResourceName,
                    accessibilityNode = node.node
                ))
            }
        }
        
        val screen = UiScreen(
            elements = elements,
            packageName = "",
            activityName = null,
            timestamp = System.currentTimeMillis()
        ).copy(screenshotHash = input.screenshotHash)
        
        return UiMap(currentScreen = screen)
    }
}
