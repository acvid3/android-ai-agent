package com.androidaiagent.replay

import com.androidaiagent.world.WorldStateSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReplaySession {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val records = mutableListOf<WorldStateSnapshot>()

    fun start(sessionId: String = "replay-${System.currentTimeMillis()}") {
        records.clear()
        _sessionId.value = sessionId
        _isRecording.value = true
    }

    fun record(snapshot: WorldStateSnapshot) {
        if (!_isRecording.value) {
            return
        }
        records.add(snapshot.copy(screenshot = null))
    }

    fun stop(): List<WorldStateSnapshot> {
        _isRecording.value = false
        _sessionId.value = null
        return records.toList()
    }

    fun clear() {
        records.clear()
        _isRecording.value = false
        _sessionId.value = null
    }
}
