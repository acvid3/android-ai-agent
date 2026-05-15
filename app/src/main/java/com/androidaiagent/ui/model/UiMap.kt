package com.androidaiagent.ui.model

data class UiMap(
    val currentScreen: UiScreen,
    val screenHistory: List<UiScreen> = emptyList(),
    val detectedRoute: String? = null,
    val routeConfidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun withNewScreen(screen: UiScreen): UiMap {
        val newHistory = if (screenHistory.size >= 10) {
            screenHistory.drop(1) + currentScreen
        } else {
            screenHistory + currentScreen
        }
        return copy(
            currentScreen = screen,
            screenHistory = newHistory
        )
    }
    
    fun withRoute(route: String, confidence: Float): UiMap {
        return copy(
            detectedRoute = route,
            routeConfidence = confidence
        )
    }
    
    fun getPreviousScreen(): UiScreen? {
        return screenHistory.lastOrNull()
    }
    
    fun hasScreenChanged(): Boolean {
        val previous = getPreviousScreen() ?: return true
        return previous.packageName != currentScreen.packageName ||
               previous.activityName != currentScreen.activityName
    }
}
