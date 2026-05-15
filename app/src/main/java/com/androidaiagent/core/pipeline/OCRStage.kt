package com.androidaiagent.core.pipeline

import android.graphics.Bitmap
import com.androidaiagent.ocr.OCREngine

class OCRStage(
    private val ocrEngine: OCREngine = OCREngine()
) : PipelineStage<Bitmap, OCRResult>() {
    override suspend fun process(input: Bitmap): OCRResult {
        val textRegions = ocrEngine.extractTextWithRegions(input).map {
            TextRegion(
                text = it.text,
                bounds = it.bounds,
                confidence = it.confidence
            )
        }
        return OCRResult(textRegions)
    }
}
