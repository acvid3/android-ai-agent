package com.androidaiagent.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityInteractionEngine(
    private val accessibilityService: AccessibilityService
) {
    fun findClickableNodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        traverse(root) { node ->
            if (node.isClickable && node.isEnabled) {
                result.add(AccessibilityNodeInfo.obtain(node))
            }
        }
        return result
    }

    fun tapByBounds(bounds: Rect): Boolean {
        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        return accessibilityService.performClick(x, y)
    }

    fun scrollContainer(root: AccessibilityNodeInfo?, forward: Boolean = true): Boolean {
        if (root == null || !root.isScrollable) return false
        return if (forward) root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        else root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    fun inputText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun detectClickableState(node: AccessibilityNodeInfo?): Boolean {
        return node?.isClickable == true && node.isEnabled
    }

    fun detectEnabledState(node: AccessibilityNodeInfo?): Boolean {
        return node?.isEnabled == true
    }

    fun performGestureTap(x: Float, y: Float): Boolean {
        return accessibilityService.performClick(x, y)
    }

    fun performGestureSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        return accessibilityService.performSwipe(x1, y1, x2, y2, duration)
    }

    fun performLongPress(x: Float, y: Float, duration: Long = 500L): Boolean {
        return accessibilityService.performLongPress(x, y, duration)
    }

    fun fallbackShellInput(text: String): Boolean {
        return try {
            val escaped = text.replace(" ", "%s")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "input text \"$escaped\""))
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun traverse(node: AccessibilityNodeInfo, block: (AccessibilityNodeInfo) -> Unit) {
        block(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { traverse(it, block) }
        }
    }
}
