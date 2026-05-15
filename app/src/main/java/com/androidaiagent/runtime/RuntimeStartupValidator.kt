package com.androidaiagent.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.androidaiagent.ai.AIProvider
import java.util.Locale

data class StartupValidationResult(
    val isValid: Boolean,
    val missingRequirements: List<String> = emptyList()
)

class RuntimeStartupValidator(
    private val context: Context
) {
    fun validate(
        accessibilityReady: Boolean,
        overlayPermissionGranted: Boolean,
        screenCaptureReady: Boolean,
        ocrReady: Boolean,
        aiProvider: AIProvider?,
        requireAiProvider: Boolean = true
    ): StartupValidationResult {
        val missing = mutableListOf<String>()

        if (!accessibilityReady) {
            missing += "accessibility"
        }

        if (!overlayPermissionGranted && !Settings.canDrawOverlays(context)) {
            missing += "overlay_permission"
        }

        if (!screenCaptureReady) {
            missing += "screen_capture"
        }

        if (!ocrReady) {
            missing += "ocr"
        }

        if (requireAiProvider && aiProvider == null) {
            missing += "ai_provider"
        }

        val accessibilityEnabled = isAccessibilityEnabled()
        if (!accessibilityEnabled) {
            missing += "accessibility_service_disabled"
        }

        return StartupValidationResult(
            isValid = missing.isEmpty(),
            missingRequirements = missing.distinct()
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val packageName = context.packageName.lowercase(Locale.US)
        return enabled.lowercase(Locale.US).contains(packageName) &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }
}
