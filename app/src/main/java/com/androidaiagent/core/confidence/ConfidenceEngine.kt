package com.androidaiagent.core.confidence

data class ConfidenceInputs(
    val ocrConfidence: Float = 0f,
    val routeConfidence: Float = 0f,
    val aiConfidence: Float = 0f,
    val detectorConfidence: Float = 0f,
    val executionConfidence: Float = 0f
)

class ConfidenceEngine(
    private val ocrWeight: Float = 0.2f,
    private val routeWeight: Float = 0.3f,
    private val aiWeight: Float = 0.2f,
    private val detectorWeight: Float = 0.15f,
    private val executionWeight: Float = 0.15f
) {
    fun fuse(inputs: ConfidenceInputs): Float {
        return (
            inputs.ocrConfidence * ocrWeight +
            inputs.routeConfidence * routeWeight +
            inputs.aiConfidence * aiWeight +
            inputs.detectorConfidence * detectorWeight +
            inputs.executionConfidence * executionWeight
        ).coerceIn(0f, 1f)
    }
}
