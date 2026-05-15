package com.androidaiagent.ui.model

import android.graphics.Rect

sealed class UiElement {
    abstract val bounds: Rect
    abstract val confidence: Float
    abstract val accessibilityNode: android.view.accessibility.AccessibilityNodeInfo?
}

data class DetectedButton(
    override val bounds: Rect,
    override val confidence: Float,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    override val accessibilityNode: android.view.accessibility.AccessibilityNodeInfo? = null,
    val isClickable: Boolean = true
) : UiElement()

data class DetectedText(
    override val bounds: Rect,
    override val confidence: Float,
    val text: String,
    val fontSize: Float? = null,
    override val accessibilityNode: android.view.accessibility.AccessibilityNodeInfo? = null
) : UiElement()

data class DetectedIcon(
    override val bounds: Rect,
    override val confidence: Float,
    val iconType: IconType,
    val resourceId: String?,
    override val accessibilityNode: android.view.accessibility.AccessibilityNodeInfo? = null
) : UiElement()

data class DetectedTextField(
    override val bounds: Rect,
    override val confidence: Float,
    val hintText: String?,
    val currentText: String?,
    val isEditable: Boolean,
    override val accessibilityNode: android.view.accessibility.AccessibilityNodeInfo? = null
) : UiElement()

data class DetectedImage(
    override val bounds: Rect,
    override val confidence: Float,
    val contentDescription: String?,
    override val accessibilityNode: android.view.accessibility.AccessibilityNodeInfo? = null
) : UiElement()

data class InteractionZone(
    override val bounds: Rect,
    override val confidence: Float = 1.0f,
    val zoneType: ZoneType,
    val isSafe: Boolean = true,
    override val accessibilityNode: android.view.accessibility.AccessibilityNodeInfo? = null
) : UiElement()

enum class IconType {
    HOME,
    BACK,
    MENU,
    SEARCH,
    SETTINGS,
    CLOSE,
    CHECK,
    UNKNOWN
}

enum class ZoneType {
    SAFE,
    DANGEROUS,
    NAVIGATION,
    CONTENT,
    SYSTEM
}
