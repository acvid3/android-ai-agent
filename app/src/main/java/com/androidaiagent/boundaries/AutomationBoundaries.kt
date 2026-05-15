package com.androidaiagent.boundaries

import android.graphics.Rect
import com.androidaiagent.ui.model.UiMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutomationBoundaries {
    private val allowedZones = mutableListOf<Rect>()
    private val forbiddenZones = mutableListOf<Rect>()
    private val appWhitelist = mutableListOf<String>()
    private val appBlacklist = mutableListOf<String>()
    
    private val _safeMode = MutableStateFlow(true)
    val safeMode: StateFlow<Boolean> = _safeMode.asStateFlow()
    
    private val _maxActionsPerMinute = MutableStateFlow(60)
    val maxActionsPerMinute: StateFlow<Int> = _maxActionsPerMinute.asStateFlow()
    
    private var actionCount = 0
    private var windowStartTime = System.currentTimeMillis()
    
    fun addAllowedZone(rect: Rect) {
        allowedZones.add(rect)
    }
    
    fun addForbiddenZone(rect: Rect) {
        forbiddenZones.add(rect)
    }
    
    fun addAppToWhitelist(packageName: String) {
        appWhitelist.add(packageName)
    }
    
    fun addAppToBlacklist(packageName: String) {
        appBlacklist.add(packageName)
    }
    
    fun isActionAllowed(x: Float, y: Float, packageName: String): Boolean {
        if (!_safeMode.value) return true
        
        if (packageName in appBlacklist) {
            return false
        }
        
        if (appWhitelist.isNotEmpty() && packageName !in appWhitelist) {
            return false
        }
        
        val point = android.graphics.Point(x.toInt(), y.toInt())
        
        for (zone in forbiddenZones) {
            if (zone.contains(point.x, point.y)) {
                return false
            }
        }
        
        if (allowedZones.isNotEmpty()) {
            val inAllowedZone = allowedZones.any { it.contains(point.x, point.y) }
            if (!inAllowedZone) {
                return false
            }
        }
        
        return checkActionRate()
    }
    
    private fun checkActionRate(): Boolean {
        val now = System.currentTimeMillis()
        val windowDuration = 60000L
        
        if (now - windowStartTime > windowDuration) {
            actionCount = 0
            windowStartTime = now
        }
        
        if (actionCount >= _maxActionsPerMinute.value) {
            return false
        }
        
        actionCount++
        return true
    }
    
    fun setSafeMode(enabled: Boolean) {
        _safeMode.value = enabled
    }
    
    fun setMaxActionsPerMinute(max: Int) {
        _maxActionsPerMinute.value = max
    }
    
    fun clearZones() {
        allowedZones.clear()
        forbiddenZones.clear()
    }
    
    fun clearAppLists() {
        appWhitelist.clear()
        appBlacklist.clear()
    }
    
    fun getAllowedZones(): List<Rect> = allowedZones.toList()
    fun getForbiddenZones(): List<Rect> = forbiddenZones.toList()
    fun getAppWhitelist(): List<String> = appWhitelist.toList()
    fun getAppBlacklist(): List<String> = appBlacklist.toList()
    
    fun resetActionCount() {
        actionCount = 0
        windowStartTime = System.currentTimeMillis()
    }
}
