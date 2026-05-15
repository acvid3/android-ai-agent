package com.androidaiagent.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PatternSuggestion(
    val appPackage: String,
    val actionType: String,
    val repeatCount: Int,
    val uiContext: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val signature: String = buildString {
        append(appPackage)
        append('|')
        append(actionType)
        append('|')
        append(uiContext.orEmpty())
        append('|')
        append(repeatCount)
    }
}

object PatternSuggestionStore {
    private val _currentSuggestion = MutableStateFlow<PatternSuggestion?>(null)
    val currentSuggestion: StateFlow<PatternSuggestion?> = _currentSuggestion.asStateFlow()

    fun showSuggestion(suggestion: PatternSuggestion) {
        val current = _currentSuggestion.value
        if (current?.signature == suggestion.signature) return
        _currentSuggestion.value = suggestion
    }

    fun dismiss() {
        _currentSuggestion.value = null
    }
}
