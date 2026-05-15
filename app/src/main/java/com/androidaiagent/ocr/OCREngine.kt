package com.androidaiagent.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

class OCREngine(
    private val confidenceThreshold: Float = 0.55f,
    private val duplicateThreshold: Int = 2
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun extractText(bitmap: Bitmap): String {
        return extractTextWithRegions(bitmap).joinToString("\n") { it.text }
    }

    fun extractTextWithRegions(bitmap: Bitmap): List<TextRegion> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = try {
            Tasks.await(recognizer.process(image), 2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            return emptyList()
        }

        return normalize(result)
    }

    private fun normalize(result: Text): List<TextRegion> {
        val seen = LinkedHashMap<String, TextRegion>()

        for (block in result.textBlocks) {
            for (line in block.lines) {
                val rawText = line.text.trim()
                val confidence = line.confidence ?: block.confidence ?: 0f
                val bounds = line.boundingBox ?: block.boundingBox ?: Rect()

                if (rawText.isBlank()) continue
                if (confidence < confidenceThreshold) continue

                val key = normalizeKey(rawText)
                val normalized = TextRegion(
                    text = rawText,
                    bounds = bounds,
                    confidence = max(confidence, 0f)
                )

                val existing = seen[key]
                if (existing == null) {
                    seen[key] = normalized
                } else {
                    if (normalizeBox(existing.bounds) == normalizeBox(bounds)) {
                        seen[key] = existing.copy(
                            confidence = max(existing.confidence, normalized.confidence)
                        )
                    }
                }
            }
        }

        val deduped = seen.values.toMutableList()
        if (duplicateThreshold > 1) {
            return deduped.distinctBy { "${normalizeKey(it.text)}:${normalizeBox(it.bounds)}" }
        }
        return deduped
    }

    private fun normalizeKey(text: String): String {
        return text.lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^\\p{L}\\p{Nd} ]"), "")
            .trim()
    }

    private fun normalizeBox(rect: Rect): String {
        return "${rect.left}:${rect.top}:${rect.right}:${rect.bottom}"
    }
}

data class TextRegion(
    val text: String,
    val bounds: Rect,
    val confidence: Float
)
