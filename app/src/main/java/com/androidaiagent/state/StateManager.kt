package com.androidaiagent.state

data class AgentState(
    val currentRoute: String? = null,
    val previousRoute: String? = null,
    val activeTask: String = "",
    val actionHistory: MutableList<String> = mutableListOf(),
    val screenHistory: MutableList<String> = mutableListOf(),
    val cooldowns: MutableMap<String, Long> = mutableMapOf(),
    val retryCounters: MutableMap<String, Int> = mutableMapOf(),
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
)

class StateManager {
    private var state = AgentState()

    fun getState(): AgentState = state

    fun updateCurrentRoute(route: String) {
        state = state.copy(
            previousRoute = state.currentRoute,
            currentRoute = route
        )
    }

    fun setActiveTask(task: String) {
        state = state.copy(activeTask = task)
    }

    fun addAction(action: String) {
        state.actionHistory.add(action)
        if (state.actionHistory.size > 100) {
            state.actionHistory.removeAt(0)
        }
    }

    fun addScreen(screen: String) {
        state.screenHistory.add(screen)
        if (state.screenHistory.size > 50) {
            state.screenHistory.removeAt(0)
        }
    }

    fun setRunning(running: Boolean) {
        state = state.copy(isRunning = running)
    }

    fun setPaused(paused: Boolean) {
        state = state.copy(isPaused = paused)
    }

    fun incrementRetry(key: String) {
        val current = state.retryCounters[key] ?: 0
        state.retryCounters[key] = current + 1
    }

    fun resetRetry(key: String) {
        state.retryCounters.remove(key)
    }

    fun setCooldown(key: String, duration: Long) {
        state.cooldowns[key] = System.currentTimeMillis() + duration
    }

    fun isOnCooldown(key: String): Boolean {
        val expiry = state.cooldowns[key] ?: return false
        return System.currentTimeMillis() < expiry
    }
}
