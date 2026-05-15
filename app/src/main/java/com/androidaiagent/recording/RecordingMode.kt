package com.androidaiagent.recording

import android.graphics.Bitmap
import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.routing.RouteDefinition
import com.androidaiagent.core.eventbus.Event
import com.androidaiagent.core.eventbus.GlobalEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RecordingMode(
    private val eventBus: com.androidaiagent.core.eventbus.EventBus = GlobalEventBus
) {
    private val recordedActions = mutableListOf<RecordedAction>()
    private val recordedScreens = mutableListOf<RecordedScreen>()
    private val recordedTransitions = mutableListOf<RecordedTransition>()
    private val recordedUiElements = mutableListOf<RecordedUiElement>()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _actionCount = MutableStateFlow(0)
    val actionCount: StateFlow<Int> = _actionCount.asStateFlow()
    
    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()
    
    private var currentRoute: String? = null
    
    fun startRecording() {
        val id = UUID.randomUUID().toString()
        _sessionId.value = id
        _isRecording.value = true
        recordedActions.clear()
        recordedScreens.clear()
        recordedTransitions.clear()
        recordedUiElements.clear()
        currentRoute = null
        
        eventBus.tryPublish(Event.RecordingStarted(id))
    }
    
    fun stopRecording() {
        val id = _sessionId.value ?: return
        _isRecording.value = false
        eventBus.tryPublish(Event.RecordingStopped(id, recordedActions.size))
    }
    
    fun recordAction(action: String, target: String?, parameters: Map<String, Any>) {
        if (!_isRecording.value) return
        
        val recordedAction = RecordedAction(
            action = action,
            target = target,
            parameters = parameters,
            route = currentRoute,
            timestamp = System.currentTimeMillis()
        )
        
        recordedActions.add(recordedAction)
        _actionCount.value = recordedActions.size
        
        eventBus.tryPublish(Event.RecordingActionRecorded(action, target))
    }
    
    fun recordScreen(bitmap: Bitmap, uiMap: UiMap) {
        if (!_isRecording.value) return
        
        val recordedScreen = RecordedScreen(
            bitmap = bitmap,
            uiMap = uiMap,
            route = currentRoute,
            timestamp = System.currentTimeMillis()
        )
        
        recordedScreens.add(recordedScreen)
        
        uiMap.currentScreen.elements.forEach { element ->
            recordedUiElements.add(RecordedUiElement(
                element = element,
                route = currentRoute,
                timestamp = System.currentTimeMillis()
            ))
        }
    }
    
    fun recordTransition(from: String?, to: String) {
        if (!_isRecording.value) return
        
        val transition = RecordedTransition(
            from = from,
            to = to,
            timestamp = System.currentTimeMillis()
        )
        
        recordedTransitions.add(transition)
        currentRoute = to
    }
    
    fun generateRouteGraph(): List<RouteDefinition> {
        val routes = mutableListOf<RouteDefinition>()
        val routeActions = mutableMapOf<String, MutableList<RecordedAction>>()
        val routeElements = mutableMapOf<String, MutableList<RecordedUiElement>>()
        
        recordedActions.forEach { action ->
            if (action.route != null) {
                routeActions.getOrPut(action.route!!) { mutableListOf() }.add(action)
            }
        }
        
        recordedUiElements.forEach { element ->
            if (element.route != null) {
                routeElements.getOrPut(element.route!!) { mutableListOf() }.add(element)
            }
        }
        
        routeActions.forEach { (routeName, actions) ->
            val buttons = actions.mapNotNull { it.target }.distinct()
            val elements = routeElements[routeName] ?: emptyList()
            val textElements = elements.mapNotNull { it.element.text }.distinct()
            
            val navigationTargets = recordedTransitions
                .filter { it.from == routeName }
                .map { it.to }
                .distinct()
                .associateWith { "transition" }
            
            val route = RouteDefinition(
                name = routeName,
                description = "Auto-generated from recording session ${_sessionId.value}",
                expectedButtons = buttons,
                expectedText = textElements,
                availableActions = actions.map { it.action }.distinct(),
                navigationTargets = navigationTargets
            )
            routes.add(route)
        }
        
        return routes
    }
    
    fun getRecordedActions(): List<RecordedAction> = recordedActions.toList()
    fun getRecordedScreens(): List<RecordedScreen> = recordedScreens.toList()
    fun getRecordedTransitions(): List<RecordedTransition> = recordedTransitions.toList()
    fun getRecordedUiElements(): List<RecordedUiElement> = recordedUiElements.toList()
    
    fun clear() {
        recordedActions.clear()
        recordedScreens.clear()
        recordedTransitions.clear()
        recordedUiElements.clear()
        currentRoute = null
        _actionCount.value = 0
        _sessionId.value = null
    }
}

data class RecordedAction(
    val action: String,
    val target: String?,
    val parameters: Map<String, Any>,
    val route: String?,
    val timestamp: Long
)

data class RecordedScreen(
    val bitmap: Bitmap,
    val uiMap: UiMap,
    val route: String?,
    val timestamp: Long
)

data class RecordedTransition(
    val from: String?,
    val to: String,
    val timestamp: Long
)

data class RecordedUiElement(
    val element: com.androidaiagent.ui.model.UiElement,
    val route: String?,
    val timestamp: Long
)
