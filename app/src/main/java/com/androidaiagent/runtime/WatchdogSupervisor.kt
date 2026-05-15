package com.androidaiagent.runtime

import com.androidaiagent.action.ActionQueue
import com.androidaiagent.metrics.HealthMonitor
import com.androidaiagent.metrics.HealthSnapshot
import com.androidaiagent.runtime.HealthStatus

enum class WatchdogAction {
    NONE,
    CLEAR_QUEUE,
    RESET_SESSION
}

class WatchdogSupervisor(
    private val healthMonitor: HealthMonitor,
    private val queue: ActionQueue? = null
) {
    fun inspect(queueSize: Int = queue?.queueSize?.value ?: 0, now: Long = System.currentTimeMillis()): Pair<WatchdogAction, HealthSnapshot> {
        val health = healthMonitor.evaluate(queueSize, now)
        val action = when (health.status) {
            HealthStatus.UNHEALTHY -> WatchdogAction.RESET_SESSION
            HealthStatus.RECOVERING -> WatchdogAction.CLEAR_QUEUE
            HealthStatus.HEALTHY -> WatchdogAction.NONE
        }
        return action to health
    }

    fun handle(action: WatchdogAction) {
        when (action) {
            WatchdogAction.NONE -> Unit
            WatchdogAction.CLEAR_QUEUE -> queue?.clear()
            WatchdogAction.RESET_SESSION -> {
                queue?.clear()
                healthMonitor.reset()
            }
        }
    }
}
