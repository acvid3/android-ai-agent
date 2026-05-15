package com.androidaiagent.humanization

import com.androidaiagent.ai.ActionType
import kotlin.math.sin
import kotlin.random.Random

class HumanizationEngine {
    private val random = Random.Default
    
    fun randomizeTapOffset(baseX: Float, baseY: Float, maxOffset: Float = 10f): Pair<Float, Float> {
        val offsetX = (random.nextDouble() - 0.5) * 2 * maxOffset
        val offsetY = (random.nextDouble() - 0.5) * 2 * maxOffset
        return (baseX + offsetX).toFloat() to (baseY + offsetY).toFloat()
    }
    
    fun generateSwipeCurve(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long
    ): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val steps = (duration / 16).toInt()
        
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val curve = calculateBezierCurve(t)
            
            val x = startX + (endX - startX) * t + curve.first
            val y = startY + (endY - startY) * t + curve.second
            
            points.add(x to y)
        }
        
        return points
    }
    
    private fun calculateBezierCurve(t: Float): Pair<Float, Float> {
        val curveIntensity = 20f
        val frequency = 2f
        
        val offsetX = sin(t * Math.PI * frequency) * curveIntensity * (1 - t)
        val offsetY = sin(t * Math.PI * frequency + Math.PI / 2) * curveIntensity * (1 - t)
        
        return offsetX.toFloat() to offsetY.toFloat()
    }
    
    fun adaptiveDelay(baseDelay: Long, actionType: ActionType): Long {
        val variance = when (actionType) {
            ActionType.TAP -> 0.3
            ActionType.SWIPE -> 0.2
            ActionType.TYPE -> 0.5
            ActionType.LONG_PRESS -> 0.4
            else -> 0.2
        }
        
        val randomFactor = (random.nextDouble() - 0.5) * 2 * variance
        return (baseDelay * (1 + randomFactor)).toLong().coerceAtLeast(50L)
    }
    
    fun calculateInteractionPacing(actionCount: Int, timeWindow: Long): Long {
        val baseInterval = 1000L
        val pacingFactor = when {
            actionCount > 30 -> 2.0
            actionCount > 20 -> 1.5
            actionCount > 10 -> 1.2
            else -> 1.0
        }
        
        return (baseInterval * pacingFactor).toLong()
    }
    
    fun simulateIdleBehavior(minDuration: Long = 2000L, maxDuration: Long = 5000L): Long {
        return random.nextLong(minDuration, maxDuration)
    }
    
    fun variableSwipeDuration(baseDuration: Long): Long {
        val variance = 0.3
        val randomFactor = (random.nextDouble() - 0.5) * 2 * variance
        return (baseDuration * (1 + randomFactor)).toLong().coerceAtLeast(200L)
    }
    
    fun randomTypingSpeed(baseSpeed: Long): Long {
        val variance = 0.4
        val randomFactor = (random.nextDouble() - 0.5) * 2 * variance
        return (baseSpeed * (1 + randomFactor)).toLong().coerceAtLeast(30L)
    }
}
