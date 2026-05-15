package com.androidaiagent.config

import android.content.Context
import com.androidaiagent.routing.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ConfigPersistence(private val context: Context) {
    private val configDir = File(context.filesDir, "configs")
    private val profilesDir = File(configDir, "profiles")
    private val taskPresetsDir = File(configDir, "tasks")
    private val routingPresetsDir = File(configDir, "routing")
    
    private val _currentProfile = MutableStateFlow("default")
    val currentProfile: StateFlow<String> = _currentProfile.asStateFlow()
    
    init {
        configDir.mkdirs()
        profilesDir.mkdirs()
        taskPresetsDir.mkdirs()
        routingPresetsDir.mkdirs()
    }
    
    fun saveProfile(profileName: String, config: AppConfig) {
        val file = File(profilesDir, "$profileName.json")
        val json = JSONObject().apply {
            put("toolConfig", JSONObject().apply {
                put("clickDelayMin", config.toolConfig.clickDelayMin)
                put("clickDelayMax", config.toolConfig.clickDelayMax)
                put("swipeDuration", config.toolConfig.swipeDuration)
                put("randomizationAmount", config.toolConfig.randomizationAmount)
                put("typingSpeed", config.toolConfig.typingSpeed)
                put("retryCount", config.toolConfig.retryCount)
                put("actionCooldown", config.toolConfig.actionCooldown)
                put("screenAnalysisInterval", config.toolConfig.screenAnalysisInterval)
                put("ocrSensitivity", config.toolConfig.ocrSensitivity)
                put("confidenceThreshold", config.toolConfig.confidenceThreshold)
                put("failRecoveryTimeout", config.toolConfig.failRecoveryTimeout)
            })
            put("systemPrompt", config.systemPrompt)
            put("aiProvider", config.aiProvider)
            put("apiKey", config.apiKey)
        }
        file.writeText(json.toString())
    }
    
    fun loadProfile(profileName: String): AppConfig? {
        val file = File(profilesDir, "$profileName.json")
        if (!file.exists()) return null
        
        val json = JSONObject(file.readText())
        val toolConfigJson = json.getJSONObject("toolConfig")
        
        val toolConfig = ToolConfig(
            clickDelayMin = toolConfigJson.getLong("clickDelayMin"),
            clickDelayMax = toolConfigJson.getLong("clickDelayMax"),
            swipeDuration = toolConfigJson.getLong("swipeDuration"),
            randomizationAmount = toolConfigJson.getDouble("randomizationAmount").toFloat(),
            typingSpeed = toolConfigJson.getLong("typingSpeed"),
            retryCount = toolConfigJson.getInt("retryCount"),
            actionCooldown = toolConfigJson.getLong("actionCooldown"),
            screenAnalysisInterval = toolConfigJson.getLong("screenAnalysisInterval"),
            ocrSensitivity = toolConfigJson.getDouble("ocrSensitivity").toFloat(),
            confidenceThreshold = toolConfigJson.getDouble("confidenceThreshold").toFloat(),
            failRecoveryTimeout = toolConfigJson.getLong("failRecoveryTimeout")
        )
        
        return AppConfig(
            toolConfig = toolConfig,
            systemPrompt = json.getString("systemPrompt"),
            aiProvider = json.getString("aiProvider"),
            apiKey = json.getString("apiKey")
        )
    }
    
    fun getProfiles(): List<String> {
        return profilesDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    }
    
    fun deleteProfile(profileName: String) {
        File(profilesDir, "$profileName.json").delete()
    }
    
    fun saveTaskPreset(presetName: String, tasks: List<com.androidaiagent.core.taskengine.TaskDefinition>) {
        val file = File(taskPresetsDir, "$presetName.json")
        val jsonArray = JSONArray()
        
        tasks.forEach { task ->
            jsonArray.put(JSONObject().apply {
                put("id", task.id)
                put("description", task.description)
                put("priority", task.priority.name)
                put("cooldownDuration", task.cooldownDuration)
            })
        }
        
        file.writeText(jsonArray.toString())
    }
    
    fun saveRoutingPreset(presetName: String, routes: List<Route>) {
        val file = File(routingPresetsDir, "$presetName.json")
        val jsonArray = JSONArray()
        
        routes.forEach { route ->
            jsonArray.put(JSONObject().apply {
                put("name", route.name)
                put("description", route.description)
                put("purpose", route.purpose)
                put("expectedButtons", JSONArray(route.expectedButtons))
                put("expectedText", JSONArray(route.expectedText))
                put("availableActions", JSONArray(route.availableActions))
                put("navigationTargets", JSONArray(route.navigationTargets))
                put("parentRoute", route.parentRoute)
                put("childRoutes", JSONArray(route.childRoutes))
            })
        }
        
        file.writeText(jsonArray.toString())
    }
    
    fun loadRoutingPreset(presetName: String): List<Route> {
        val file = File(routingPresetsDir, "$presetName.json")
        if (!file.exists()) return emptyList()
        
        val jsonArray = JSONArray(file.readText())
        val routes = mutableListOf<Route>()
        
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val childRoutes = mutableListOf<String>()
            val childRoutesJson = json.optJSONArray("childRoutes")
            if (childRoutesJson != null) {
                for (j in 0 until childRoutesJson.length()) {
                    childRoutes.add(childRoutesJson.getString(j))
                }
            }
            
            val route = Route(
                name = json.getString("name"),
                description = json.getString("description"),
                purpose = json.getString("purpose"),
                expectedButtons = json.getJSONArray("expectedButtons").let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                expectedText = json.getJSONArray("expectedText").let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                availableActions = json.getJSONArray("availableActions").let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                navigationTargets = json.getJSONArray("navigationTargets").let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                parentRoute = json.optString("parentRoute"),
                childRoutes = childRoutes
            )
            routes.add(route)
        }
        
        return routes
    }
    
    fun exportConfig(profileName: String): File {
        val file = File(configDir, "export_$profileName.json")
        val profile = loadProfile(profileName) ?: return file
        
        val json = JSONObject().apply {
            put("profile", profileName)
            put("config", JSONObject().apply {
                put("toolConfig", JSONObject().apply {
                    put("clickDelayMin", profile.toolConfig.clickDelayMin)
                    put("clickDelayMax", profile.toolConfig.clickDelayMax)
                })
                put("systemPrompt", profile.systemPrompt)
                put("aiProvider", profile.aiProvider)
            })
        }
        
        file.writeText(json.toString())
        return file
    }
    
    fun importConfig(file: File): Boolean {
        try {
            val json = JSONObject(file.readText())
            val profileName = json.getString("profile")
            
            val configJson = json.getJSONObject("config")
            val toolConfigJson = configJson.getJSONObject("toolConfig")
            
            val toolConfig = ToolConfig(
                clickDelayMin = toolConfigJson.getLong("clickDelayMin"),
                clickDelayMax = toolConfigJson.getLong("clickDelayMax"),
                swipeDuration = 300L,
                randomizationAmount = 0.1f,
                typingSpeed = 50L,
                retryCount = 3,
                actionCooldown = 500L,
                screenAnalysisInterval = 1000L,
                ocrSensitivity = 0.7f,
                confidenceThreshold = 0.6f,
                failRecoveryTimeout = 5000L
            )
            
            val config = AppConfig(
                toolConfig = toolConfig,
                systemPrompt = configJson.getString("systemPrompt"),
                aiProvider = configJson.getString("aiProvider"),
                apiKey = ""
            )
            
            saveProfile(profileName, config)
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    fun setCurrentProfile(profileName: String) {
        _currentProfile.value = profileName
    }
}
