package com.androidaiagent.core.eventbus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class EventHandler(
    private val eventBus: EventBus = GlobalEventBus,
    private val scope: CoroutineScope
) {
    private var job: Job? = null
    
    abstract suspend fun handle(event: Event)
    
    fun start() {
        job = scope.launch {
            eventBus.eventFlow.collectLatest { event ->
                try {
                    handle(event)
                } catch (e: Exception) {
                    eventBus.tryPublish(Event.ErrorOccurred(e.message ?: "Unknown error", javaClass.simpleName))
                }
            }
        }
    }
    
    fun stop() {
        job?.cancel()
        job = null
    }
}
