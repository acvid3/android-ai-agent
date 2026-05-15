package com.androidaiagent.action

data class ExecutionResult(
    val actionId: String,
    val sourceFrameId: Long,
    val resultFrameId: Long,
    val beforeRoute: String?,
    val afterRoute: String?,
    val detector: String,
    val verification: VerificationResult,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
