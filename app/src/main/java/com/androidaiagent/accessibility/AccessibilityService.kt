package com.androidaiagent.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.androidaiagent.settings.AppSettingsStore
import com.androidaiagent.tracking.UserActionRecord
import com.androidaiagent.tracking.UserActionTracker

class AccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "AccessibilityService"
        private var currentPackageName: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected: AccessibilityService connected")
        UserActionTracker.init(applicationContext)

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Log.d(TAG, "onServiceConnected: Service info configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentEvent = event ?: return
        if (!AppSettingsStore.isTrackingEnabled(applicationContext)) return

        val packageName = currentEvent.packageName?.toString() ?: return

        // Log packageName changes for validation
        if (currentPackageName != packageName) {
            Log.d(TAG, "Package changed: $currentPackageName -> $packageName")
            currentPackageName = packageName
        }

        val actionType = when (currentEvent.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "CLICK"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TEXT_INPUT"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "SCREEN_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "CONTENT_CHANGED"
            else -> return
        }

        val uiContext = buildUiContext(currentEvent)
        val text = currentEvent.text?.joinToString(" ")?.takeIf { it.isNotBlank() }

        Log.d(TAG, "Event: $actionType | Package: $packageName | Context: $uiContext | Text: $text")

        UserActionTracker.record(
            UserActionRecord(
                appPackage = packageName,
                timestamp = System.currentTimeMillis(),
                actionType = actionType,
                uiContext = uiContext,
                text = text
            )
        )
    }

    private fun buildUiContext(event: AccessibilityEvent): String? {
        val source = event.source ?: return event.className?.toString()
        return try {
            val parts = listOfNotNull(
                source.className?.toString(),
                source.viewIdResourceName,
                source.text?.toString()
            )
            parts.takeIf { it.isNotEmpty() }?.joinToString("|")
        } finally {
            source.recycle()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt: AccessibilityService interrupted")
    }
}
