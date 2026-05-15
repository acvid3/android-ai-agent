package com.androidaiagent.core.eventbus

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class EventBus {
    private val eventChannel = Channel<Event>(capacity = Channel.UNLIMITED)
    
    val eventFlow: Flow<Event> = eventChannel.receiveAsFlow()
    
    suspend fun publish(event: Event) {
        eventChannel.send(event)
    }
    
    fun tryPublish(event: Event): Boolean {
        return eventChannel.trySend(event).isSuccess
    }
}

object GlobalEventBus : EventBus()
