package com.androidaiagent.settings

import android.content.Context

object AppSettingsStore {
    private const val PREFS_NAME = "android_ai_agent_settings"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_TRACKING_ENABLED = "tracking_enabled"

    fun save(context: Context, apiKey: String, trackingEnabled: Boolean) {
        prefs(context).edit()
            .putString(KEY_API_KEY, apiKey)
            .putBoolean(KEY_TRACKING_ENABLED, trackingEnabled)
            .apply()
    }

    fun getApiKey(context: Context): String {
        return prefs(context).getString(KEY_API_KEY, "").orEmpty()
    }

    fun isTrackingEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_TRACKING_ENABLED, false)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
