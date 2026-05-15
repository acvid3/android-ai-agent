package com.androidaiagent.plugins

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface Plugin {
    val id: String
    val name: String
    val version: String
    val description: String
    
    suspend fun initialize()
    suspend fun shutdown()
}

interface TaskPlugin : Plugin {
    suspend fun executeTask(taskId: String, parameters: Map<String, Any>): Boolean
}

interface RoutingPlugin : Plugin {
    fun getRoutes(): List<com.androidaiagent.routing.Route>
    fun identifyRoute(screenData: String): String?
}

interface AIProviderPlugin : Plugin {
    suspend fun makeDecision(request: com.androidaiagent.ai.AIRequest): com.androidaiagent.ai.AIResponse
}

interface DetectorPlugin : Plugin {
    suspend fun detect(bitmap: android.graphics.Bitmap): List<com.androidaiagent.ui.model.UiElement>
}

class PluginManager {
    private val plugins = mutableMapOf<String, Plugin>()
    private val taskPlugins = mutableMapOf<String, TaskPlugin>()
    private val routingPlugins = mutableMapOf<String, RoutingPlugin>()
    private val aiProviderPlugins = mutableMapOf<String, AIProviderPlugin>()
    private val detectorPlugins = mutableMapOf<String, DetectorPlugin>()
    
    private val _loadedPlugins = MutableStateFlow<List<Plugin>>(emptyList())
    val loadedPlugins: StateFlow<List<Plugin>> = _loadedPlugins.asStateFlow()
    
    suspend fun registerPlugin(plugin: Plugin) {
        plugin.initialize()
        plugins[plugin.id] = plugin
        
        when (plugin) {
            is TaskPlugin -> taskPlugins[plugin.id] = plugin
            is RoutingPlugin -> routingPlugins[plugin.id] = plugin
            is AIProviderPlugin -> aiProviderPlugins[plugin.id] = plugin
            is DetectorPlugin -> detectorPlugins[plugin.id] = plugin
        }
        
        _loadedPlugins.value = plugins.values.toList()
    }
    
    suspend fun unregisterPlugin(pluginId: String) {
        val plugin = plugins[pluginId] ?: return
        
        plugin.shutdown()
        
        plugins.remove(pluginId)
        taskPlugins.remove(pluginId)
        routingPlugins.remove(pluginId)
        aiProviderPlugins.remove(pluginId)
        detectorPlugins.remove(pluginId)
        
        _loadedPlugins.value = plugins.values.toList()
    }
    
    fun getTaskPlugin(id: String): TaskPlugin? = taskPlugins[id]
    fun getRoutingPlugin(id: String): RoutingPlugin? = routingPlugins[id]
    fun getAIProviderPlugin(id: String): AIProviderPlugin? = aiProviderPlugins[id]
    fun getDetectorPlugin(id: String): DetectorPlugin? = detectorPlugins[id]
    
    fun getAllTaskPlugins(): List<TaskPlugin> = taskPlugins.values.toList()
    fun getAllRoutingPlugins(): List<RoutingPlugin> = routingPlugins.values.toList()
    fun getAllAIProviderPlugins(): List<AIProviderPlugin> = aiProviderPlugins.values.toList()
    fun getAllDetectorPlugins(): List<DetectorPlugin> = detectorPlugins.values.toList()
    
    suspend fun shutdownAll() {
        plugins.values.forEach { it.shutdown() }
        plugins.clear()
        taskPlugins.clear()
        routingPlugins.clear()
        aiProviderPlugins.clear()
        detectorPlugins.clear()
        _loadedPlugins.value = emptyList()
    }
}
