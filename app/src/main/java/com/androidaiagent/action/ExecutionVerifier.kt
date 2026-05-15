package com.androidaiagent.action

import com.androidaiagent.ai.ActionType
import com.androidaiagent.routing.RouteMatch
import com.androidaiagent.ui.model.UiMap

class ExecutionVerifier {
    fun verify(
        action: QueuedAction,
        before: UiMap?,
        after: UiMap?,
        beforeRoute: String?,
        afterRoute: RouteMatch?
    ): VerificationResult {
        val uiChanged = before?.currentScreen?.screenshotHash != after?.currentScreen?.screenshotHash ||
            before?.currentScreen?.elements?.size != after?.currentScreen?.elements?.size
        val routeChanged = beforeRoute != afterRoute?.routeName
        val expectedResult = when (action.action) {
            ActionType.BACK -> routeChanged || afterRoute?.routeName == "unknown"
            ActionType.TAP, ActionType.SWIPE, ActionType.LONG_PRESS -> uiChanged || routeChanged
            ActionType.TYPE -> uiChanged
            else -> true
        }

        return when {
            expectedResult -> VerificationResult(
                status = VerificationStatus.CONFIRMED,
                uiChanged = uiChanged,
                routeChanged = routeChanged,
                reason = "Execution produced expected change"
            )
            uiChanged || routeChanged -> VerificationResult(
                status = VerificationStatus.AWAITING_RESULT,
                uiChanged = uiChanged,
                routeChanged = routeChanged,
                reason = "Change observed but not fully confirmed"
            )
            else -> VerificationResult(
                status = VerificationStatus.FAILED,
                uiChanged = false,
                routeChanged = false,
                reason = "No visible result after action"
            )
        }
    }
}

data class VerificationResult(
    val status: VerificationStatus,
    val uiChanged: Boolean,
    val routeChanged: Boolean,
    val reason: String
)

enum class VerificationStatus {
    CONFIRMED,
    AWAITING_RESULT,
    FAILED,
    RECOVERED
}
