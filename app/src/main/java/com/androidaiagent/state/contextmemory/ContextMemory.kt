package com.androidaiagent.state.contextmemory

import com.androidaiagent.ui.model.UiMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContextMemory {
    private val actionHistory = mutableListOf<ContextAction>()
    private val routeTransitions = mutableListOf<RouteTransition>()
    private val failedAttempts = mutableListOf<FailedAttempt>()
    private val activeGoals = mutableListOf<ActiveGoal>()
    
    private val _currentGoal = MutableStateFlow<String?>(null)
    val currentGoal: StateFlow<String?> = _currentGoal.asStateFlow()
    
    private val _lastAction = MutableStateFlow<ContextAction?>(null)
    val lastAction: StateFlow<ContextAction?> = _lastAction.asStateFlow()
    
    fun recordAction(action: String, target: String?, route: String?, success: Boolean) {
        val contextAction = ContextAction(
            action = action,
            target = target,
            route = route,
            success = success,
            timestamp = System.currentTimeMillis()
        )
        
        actionHistory.add(contextAction)
        _lastAction.value = contextAction
        
        if (actionHistory.size > 200) {
            actionHistory.removeAt(0)
        }
        
        if (!success) {
            failedAttempts.add(FailedAttempt(action, target, route, contextAction.timestamp))
        }
    }
    
    fun recordRouteTransition(from: String?, to: String) {
        val transition = RouteTransition(from, to, System.currentTimeMillis())
        routeTransitions.add(transition)
        
        if (routeTransitions.size > 100) {
            routeTransitions.removeAt(0)
        }
    }
    
    fun setActiveGoal(goal: String) {
        _currentGoal.value = goal
        val existingGoal = activeGoals.find { it.description == goal }
        if (existingGoal != null) {
            activeGoals.remove(existingGoal)
            activeGoals.add(existingGoal.copy(lastUpdated = System.currentTimeMillis()))
        } else {
            activeGoals.add(ActiveGoal(goal, System.currentTimeMillis()))
        }
    }
    
    fun completeGoal(goal: String, success: Boolean) {
        activeGoals.removeIf { it.description == goal }
        if (goal == _currentGoal.value) {
            _currentGoal.value = null
        }
    }
    
    fun getRecentActions(count: Int = 10): List<ContextAction> {
        return actionHistory.takeLast(count)
    }
    
    fun getFailedAttempts(route: String?): List<FailedAttempt> {
        return if (route != null) {
            failedAttempts.filter { it.route == route }
        } else {
            failedAttempts
        }
    }
    
    fun hasTriedAction(action: String, target: String?, route: String?): Boolean {
        return actionHistory.any {
            it.action == action && 
            it.target == target && 
            it.route == route
        }
    }
    
    fun getRouteTransitionHistory(): List<RouteTransition> {
        return routeTransitions
    }
    
    fun whatWasTried(route: String?): List<String> {
        return actionHistory
            .filter { it.route == route }
            .map { "${it.action} on ${it.target}" }
            .distinct()
    }
    
    fun getContextSummary(): ContextSummary {
        return ContextSummary(
            currentGoal = _currentGoal.value,
            recentActions = getRecentActions(5),
            recentTransitions = routeTransitions.takeLast(5),
            failedAttempts = failedAttempts.takeLast(5),
            activeGoals = activeGoals
        )
    }
    
    fun clear() {
        actionHistory.clear()
        routeTransitions.clear()
        failedAttempts.clear()
        activeGoals.clear()
        _currentGoal.value = null
        _lastAction.value = null
    }
}

data class ContextAction(
    val action: String,
    val target: String?,
    val route: String?,
    val success: Boolean,
    val timestamp: Long
)

data class RouteTransition(
    val from: String?,
    val to: String,
    val timestamp: Long
)

data class FailedAttempt(
    val action: String,
    val target: String?,
    val route: String?,
    val timestamp: Long
)

data class ActiveGoal(
    val description: String,
    val createdAt: Long,
    val lastUpdated: Long = createdAt
)

data class ContextSummary(
    val currentGoal: String?,
    val recentActions: List<ContextAction>,
    val recentTransitions: List<RouteTransition>,
    val failedAttempts: List<FailedAttempt>,
    val activeGoals: List<ActiveGoal>
)
