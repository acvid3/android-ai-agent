package com.androidaiagent.recovery

import android.graphics.Bitmap
import com.androidaiagent.ocr.OCREngine
import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.vision.ModalDetectionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnknownStateResolver(
    private val ocrEngine: OCREngine = OCREngine(),
    private val modalDetectionEngine: ModalDetectionEngine = ModalDetectionEngine()
) {
    private val _lastResolution = MutableStateFlow<UnknownResolution?>(null)
    val lastResolution: StateFlow<UnknownResolution?> = _lastResolution.asStateFlow()

    fun resolve(uiMap: UiMap, screenshot: Bitmap? = null): UnknownResolution {
        val modal = modalDetectionEngine.detect(uiMap)
        val ocrText = screenshot?.let { ocrEngine.extractText(it) }.orEmpty()

        val resolution = UnknownResolution(
            shouldPause = true,
            reason = when {
                modal.blocking -> "blocking_modal:${modal.kind.name.lowercase()}"
                ocrText.isBlank() -> "no_ocr_signal"
                else -> "unknown_state"
            },
            detectedModal = modal,
            ocrText = ocrText
        )
        _lastResolution.value = resolution
        return resolution
    }
}

data class UnknownResolution(
    val shouldPause: Boolean,
    val reason: String,
    val detectedModal: ModalDetectionResult,
    val ocrText: String = ""
)
