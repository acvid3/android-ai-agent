package com.androidaiagent.coordinates

import android.graphics.Rect
import kotlin.math.roundToInt

class CoordinateSpaceManager(
    private val deviceWidth: Int,
    private val deviceHeight: Int,
    private val screenshotWidth: Int,
    private val screenshotHeight: Int,
    private val densityDpi: Int
) {
    fun normalizeScreenshotToDevice(x: Float, y: Float): PointF {
        val scaleX = deviceWidth.toFloat() / screenshotWidth.toFloat()
        val scaleY = deviceHeight.toFloat() / screenshotHeight.toFloat()
        return PointF(x * scaleX, y * scaleY)
    }

    fun normalizeDeviceToScreenshot(x: Float, y: Float): PointF {
        val scaleX = screenshotWidth.toFloat() / deviceWidth.toFloat()
        val scaleY = screenshotHeight.toFloat() / deviceHeight.toFloat()
        return PointF(x * scaleX, y * scaleY)
    }

    fun normalizeRect(rect: Rect): Rect {
        val topLeft = normalizeScreenshotToDevice(rect.left.toFloat(), rect.top.toFloat())
        val bottomRight = normalizeScreenshotToDevice(rect.right.toFloat(), rect.bottom.toFloat())
        return Rect(
            topLeft.x.roundToInt(),
            topLeft.y.roundToInt(),
            bottomRight.x.roundToInt(),
            bottomRight.y.roundToInt()
        )
    }

    fun normalizeOverlayToDevice(x: Float, y: Float): PointF {
        val densityScale = densityDpi / 160f
        return PointF(x * densityScale, y * densityScale)
    }

    fun rotatePoint(point: PointF, rotationDegrees: Int): PointF {
        return when (rotationDegrees % 360) {
            90 -> PointF(deviceHeight - point.y, point.x)
            180 -> PointF(deviceWidth - point.x, deviceHeight - point.y)
            270 -> PointF(point.y, deviceWidth - point.x)
            else -> point
        }
    }
}

data class PointF(val x: Float, val y: Float)
