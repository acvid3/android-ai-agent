package com.androidaiagent.core.taskengine

import com.androidaiagent.ai.ActionType
import com.androidaiagent.action.ActionQueue
import com.androidaiagent.runtime.RuntimeMode
import com.androidaiagent.routing.RouteDefinition
import kotlinx.coroutines.delay

object DemoAutomationTasks {
    const val REWARDS_TASK_ID = "demo_rewards_collect"

    fun createRoutes(): List<RouteDefinition> {
        return listOf(
            RouteDefinition(
                name = "home",
                description = "Demo home screen",
                packageName = "com.androidaiagent.demo",
                expectedText = listOf("Home", "Rewards", "Inventory", "Settings"),
                availableActions = listOf("tap_rewards", "tap_inventory", "tap_settings"),
                navigationTargets = mapOf("rewards" to "rewards", "inventory" to "inventory", "settings" to "settings")
            ),
            RouteDefinition(
                name = "rewards",
                description = "Rewards screen",
                packageName = "com.androidaiagent.demo",
                expectedText = listOf("Rewards", "Collect"),
                availableActions = listOf("collect_reward", "close_popup"),
                parentRoute = "home",
                navigationTargets = mapOf("home" to "home", "popup" to "popup")
            ),
            RouteDefinition(
                name = "popup",
                description = "Popup confirmation",
                packageName = "com.androidaiagent.demo",
                expectedText = listOf("Confirm", "Cancel"),
                availableActions = listOf("confirm", "cancel"),
                parentRoute = "rewards",
                navigationTargets = mapOf("rewards" to "rewards")
            ),
            RouteDefinition(
                name = "inventory",
                description = "Inventory screen",
                packageName = "com.androidaiagent.demo",
                expectedText = listOf("Inventory"),
                availableActions = listOf("back"),
                parentRoute = "home",
                navigationTargets = mapOf("home" to "home")
            ),
            RouteDefinition(
                name = "settings",
                description = "Settings screen",
                packageName = "com.androidaiagent.demo",
                expectedText = listOf("Settings"),
                availableActions = listOf("back"),
                parentRoute = "home",
                navigationTargets = mapOf("home" to "home")
            )
        )
    }

    fun createRewardsTask(queue: ActionQueue): TaskDefinition {
        val contract = TaskCompletionContract(
            successConditions = listOf("route=rewards", "reward_collected", "modal_confirmed"),
            failureConditions = listOf("unknown_state", "unsafe_state", "timeout"),
            retryBudget = 2,
            rollbackStrategy = "back_to_home"
        )

        return TaskDefinition(
            id = REWARDS_TASK_ID,
            description = "Open Rewards screen and collect reward",
            priority = TaskPriority.HIGH,
            goal = "Open rewards and collect reward",
            requiredRoutes = listOf("home", "rewards", "popup"),
            expectedTransitions = listOf("home->rewards", "rewards->popup", "popup->rewards"),
            validationRules = listOf(
                TaskValidationRule("deterministic_only", true, "AI remains disabled for the demo slice"),
                TaskValidationRule("route_present", true, "Rewards route must be registered")
            ),
            completionContract = contract,
            parameters = mapOf(
                "mode" to RuntimeMode.FULL_AUTONOMOUS.name
            ),
            execute = { _, _ ->
                queue.enqueue(ActionType.TAP, "Rewards", emptyMap(), source = "demo", route = "home", confidence = 1f)
                queue.enqueue(ActionType.TAP, "Collect", emptyMap(), source = "demo", route = "rewards", confidence = 1f)
                queue.enqueue(ActionType.TAP, "Confirm", emptyMap(), source = "demo", route = "popup", confidence = 1f)
                delay(10)
            }
        )
    }
}
