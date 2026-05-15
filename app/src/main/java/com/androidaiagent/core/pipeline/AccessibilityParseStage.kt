package com.androidaiagent.core.pipeline

import android.graphics.Bitmap

class AccessibilityParseStage : PipelineStage<Bitmap, AccessibilityResult>() {
    override suspend fun process(input: Bitmap): AccessibilityResult {
        val nodes = mutableListOf<AccessibilityNode>()
        
        return AccessibilityResult(nodes)
    }
}
