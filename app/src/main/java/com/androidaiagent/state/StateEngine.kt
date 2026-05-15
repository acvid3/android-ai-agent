package com.androidaiagent.state

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.routing.RouteMatch
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StateEngine(
    private val eventBus: com.androidaiagent.core.eventbus.EventBus = GlobalEventBus
) {
    private val stateTransitions = mutableListOf<StateTransition>()
    private val stateRules = mutableMapOf<String, StateRule>()
    
    private val _currentState = MutableStateFlow<AppState>(AppState.IDLE)
    val currentState: StateFlow<AppState> = _currentState.asStateFlow()
    
    private val _previousState = MutableStateFlow<AppState?>(null)
    val previousState: StateFlow<AppState?> = _previousState.asStateFlow()
    
    private val _isInKnownState = MutableStateFlow(false)
    val isInKnownState: StateFlow<Boolean> = _isInKnownState.asStateFlow()
    
    private val _stateConfidence = MutableStateFlow(0f)
    val stateConfidence: StateFlow<Float> = _stateConfidence.asStateFlow()
    
    fun registerRule(rule: StateRule) {
        stateRules[rule.stateName] = rule
    }
    
    fun registerRules(rules: List<StateRule>) {
        rules.forEach { registerRule(it) }
    }
    
    fun evaluateState(uiMap: UiMap, routeMatch: RouteMatch): AppState {
        val previous = _currentState.value
        
        val newState = when {
            routeMatch.confidence > 0.8f && routeMatch.routeName != "unknown" -> {
                determineStateFromRoute(routeMatch.routeName)
            }
            uiMap.currentScreen.elements.isEmpty() -> AppState.UNKNOWN
            uiMap.routeConfidence < 0.3f -> AppState.UNCERTAIN
            else -> AppState.KNOWN
        }
        
        if (previous != newState) {
            handleStateTransition(previous, newState, uiMap)
        }
        
        _currentState.value = newState
        _previousState.value = previous
        _isInKnownState.value = newState == AppState.KNOWN
        _stateConfidence.value = calculateStateConfidence(newState, routeMatch)
        
        return newState
    }
    
    private fun determineStateFromRoute(routeName: String): AppState {
        return when {
            routeName.contains("home", ignoreCase = true) -> AppState.HOME
            routeName.contains("menu", ignoreCase = true) -> AppState.MENU
            routeName.contains("dialog", ignoreCase = true) -> AppState.DIALOG
            routeName.contains("loading", ignoreCase = true) -> AppState.LOADING
            else -> AppState.KNOWN
        }
    }
    
    private fun calculateStateConfidence(state: AppState, routeMatch: RouteMatch): Float {
        return when (state) {
            AppState.KNOWN -> routeMatch.confidence
            AppState.HOME, AppState.MENU -> routeMatch.confidence * 0.9f
            AppState.DIALOG -> routeMatch.confidence * 0.8f
            AppState.LOADING -> 0.5f
            AppState.UNCERTAIN -> routeMatch.confidence * 0.3f
            AppState.UNKNOWN -> 0f
            AppState.IDLE -> 0f
        }
    }
    
    private fun handleStateTransition(from: AppState, to: AppState, uiMap: UiMap) {
        val transition = StateTransition(from, to, System.currentTimeMillis(), uiMap.detectedRoute)
        stateTransitions.add(transition)
        
        if (stateTransitions.size > 100) {
            stateTransitions.removeAt(0)
        }
        
        eventBus.tryPublish(Event.StateChanged(from.name, to.name))
        
        val rule = stateRules[to.name]
        if (rule != null) {
            rule.onEnter(to, uiMap)
        }
    }
    
    fun canTransition(from: AppState, to: AppState): Boolean {
        val rule = stateRules[to.name] ?: return true
        return rule.allowedFromStates.contains(from)
    }
    
    fun getStateHistory(): List<StateTransition> {
        return stateTransitions.toList()
    }
    
    fun clearHistory() {
        stateTransitions.clear()
    }
    
    fun reset() {
        _currentState.value = AppState.IDLE
        _previousState.value = null
        _isInKnownState.value = false
        _stateConfidence.value = 0f
    }
}

enum class AppState {
    IDLE,
    HOME,
    MENU,
    DIALOG,
    LOADING,
    KNOWN,
    UNCERTAIN,
    UNKNOWN
}

data class StateRule(
    val stateName: String,
    val allowedFromStates: List<AppState> = AppState.values().toList(),
    val onEnter: (AppState, UiMap) -> Unit = { _, _ -> }
)

data class StateTransition(
    val from: AppState,
    val to: AppState,
    val timestamp: Long,
    val route: String?
)
