package com.androidaiagent.core.pipeline

import android.graphics.Bitmap

class TemplateMatchingStage : PipelineStage<Pair<Bitmap, OCRResult>, List<TemplateMatch>>() {
    override suspend fun process(input: Pair<Bitmap, OCRResult>): List<TemplateMatch> {
        return emptyList()
    }
}
