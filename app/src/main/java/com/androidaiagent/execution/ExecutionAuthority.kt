package com.androidaiagent.execution

import com.androidaiagent.action.ActionQueue
import com.androidaiagent.ai.AIResponse
import com.androidaiagent.ui.model.UiMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ExecutionAuthority(
    private val actionQueue: ActionQueue,
    private val safeExecutionZone: SafeExecutionZone = SafeExecutionZone()
) {
    private val executionMutex = Mutex()

    suspend fun submit(
        response: AIResponse,
        priority: com.androidaiagent.action.ActionPriority = com.androidaiagent.action.ActionPriority.NORMAL,
        source: String = "ai",
        route: String? = null,
        confidence: Float = 0f,
        uiMap: UiMap? = null
    ): Boolean {
        if (!safeExecutionZone.allows(response.action, response.target, response.parameters, uiMap)) {
            return false
        }

        executionMutex.withLock {
            actionQueue.enqueue(response, priority, source, route, confidence)
        }
        return true
    }

    suspend fun <T> executeExclusive(block: suspend () -> T): T {
        return executionMutex.withLock { block() }
    }
}
