package com.androidaiagent.action

import com.androidaiagent.ai.ActionType
import com.androidaiagent.ai.AIResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedList
import java.util.UUID

class ActionQueue {
    private val queue = LinkedList<QueuedAction>()
    
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()
    
    private val _currentAction = MutableStateFlow<QueuedAction?>(null)
    val currentAction: StateFlow<QueuedAction?> = _currentAction.asStateFlow()
    
    fun enqueue(
        action: ActionType,
        target: String?,
        parameters: Map<String, Any>,
        priority: ActionPriority = ActionPriority.NORMAL,
        source: String = "ai",
        route: String? = null,
        confidence: Float = 0f
    ) {
        val queuedAction = QueuedAction(
            action = action,
            target = target,
            parameters = parameters,
            priority = priority,
            source = source,
            route = route,
            confidence = confidence
        )
        
        when (priority) {
            ActionPriority.CRITICAL -> queue.addFirst(queuedAction)
            ActionPriority.NORMAL -> queue.addLast(queuedAction)
            ActionPriority.LOW -> queue.addLast(queuedAction)
        }
        
        _queueSize.value = queue.size
    }
    
    fun enqueue(
        response: AIResponse,
        priority: ActionPriority = ActionPriority.NORMAL,
        source: String = "ai",
        route: String? = null,
        confidence: Float = 0f
    ) {
        enqueue(response.action, response.target, response.parameters, priority, source, route, confidence)
    }

    fun dequeue(): QueuedAction? {
        val action = queue.pollFirst() ?: return null
        action.lifecycleState = ActionLifecycleState.VALIDATING
        action.metrics.validationStartedAt = System.currentTimeMillis()
        _currentAction.value = action
        _queueSize.value = queue.size
        return action
    }
    
    fun peek(): QueuedAction? = queue.firstOrNull()

    fun isEmpty(): Boolean = queue.isEmpty()

    fun pause() {
        _currentAction.value = null
    }
    
    fun resume() = Unit

    fun markProcessed() {
        _currentAction.value = null
    }

    fun markExecuting(actionId: String) {
        updateLifecycle(actionId, ActionLifecycleState.EXECUTING) {
            it.metrics.executionStartedAt = System.currentTimeMillis()
        }
    }

    fun markAwaitingResult(actionId: String) {
        updateLifecycle(actionId, ActionLifecycleState.AWAITING_RESULT) {
            it.metrics.awaitingResultAt = System.currentTimeMillis()
        }
    }

    fun markConfirmed(actionId: String) {
        updateLifecycle(actionId, ActionLifecycleState.CONFIRMED) {
            it.metrics.completedAt = System.currentTimeMillis()
        }
    }

    fun markFailed(actionId: String, reason: String? = null) {
        updateLifecycle(actionId, ActionLifecycleState.FAILED) {
            it.metrics.completedAt = System.currentTimeMillis()
            it.metrics.failureReason = reason
        }
    }

    fun markRecovered(actionId: String) {
        updateLifecycle(actionId, ActionLifecycleState.RECOVERED) {
            it.metrics.completedAt = System.currentTimeMillis()
        }
    }

    fun clear() {
        queue.clear()
        _queueSize.value = 0
        _currentAction.value = null
    }
    
    fun getQueueSnapshot(): List<QueuedAction> {
        return queue.toList()
    }

    private fun updateLifecycle(actionId: String, state: ActionLifecycleState, update: (QueuedAction) -> Unit) {
        val current = _currentAction.value
        if (current?.id == actionId) {
            current.lifecycleState = state
            update(current)
            _currentAction.value = current
            return
        }

        queue.firstOrNull { it.id == actionId }?.let {
            it.lifecycleState = state
            update(it)
        }
    }
}

data class QueuedAction(
    val id: String = UUID.randomUUID().toString(),
    val action: ActionType,
    val target: String?,
    val parameters: Map<String, Any>,
    val priority: ActionPriority,
    val source: String,
    val route: String? = null,
    val confidence: Float = 0f,
    val retryPolicy: RetryPolicy = RetryPolicy(),
    var lifecycleState: ActionLifecycleState = ActionLifecycleState.QUEUED,
    val metrics: ActionMetrics = ActionMetrics(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class ActionPriority {
    CRITICAL,
    NORMAL,
    LOW
}

enum class ActionLifecycleState {
    QUEUED,
    VALIDATING,
    EXECUTING,
    AWAITING_RESULT,
    CONFIRMED,
    FAILED,
    RECOVERED
}

data class RetryPolicy(
    val maxRetries: Int = 2,
    val cooldownMs: Long = 0L,
    val backoffMultiplier: Float = 1.5f
)

data class ActionMetrics(
    var validationStartedAt: Long = 0L,
    var executionStartedAt: Long = 0L,
    var awaitingResultAt: Long = 0L,
    var completedAt: Long = 0L,
    var failureReason: String? = null
)
