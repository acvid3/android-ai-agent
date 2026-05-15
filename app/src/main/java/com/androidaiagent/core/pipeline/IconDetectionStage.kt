package com.androidaiagent.core.pipeline

import android.graphics.Bitmap

class IconDetectionStage : PipelineStage<Pair<Bitmap, AccessibilityResult>, List<com.androidaiagent.ui.model.DetectedIcon>>() {
    override suspend fun process(input: Pair<Bitmap, AccessibilityResult>): List<com.androidaiagent.ui.model.DetectedIcon> {
        return emptyList()
    }
}
