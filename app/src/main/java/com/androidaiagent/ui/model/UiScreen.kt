package com.androidaiagent.ui.model

data class UiScreen(
    val elements: List<UiElement>,
    val packageName: String,
    val activityName: String?,
    val timestamp: Long,
    val screenshotHash: String? = null
) {
    val buttons: List<DetectedButton>
        get() = elements.filterIsInstance<DetectedButton>()
    
    val textFields: List<DetectedTextField>
        get() = elements.filterIsInstance<DetectedTextField>()
    
    val textElements: List<DetectedText>
        get() = elements.filterIsInstance<DetectedText>()
    
    val icons: List<DetectedIcon>
        get() = elements.filterIsInstance<DetectedIcon>()
    
    val interactionZones: List<InteractionZone>
        get() = elements.filterIsInstance<InteractionZone>()
    
    fun findElementAt(x: Float, y: Float): UiElement? {
        return elements.find { it.bounds.contains(x.toInt(), y.toInt()) }
    }
    
    fun findElementsByText(text: String, ignoreCase: Boolean = true): List<UiElement> {
        return elements.filter { element ->
            when (element) {
                is DetectedButton -> element.text?.contains(text, ignoreCase) == true ||
                                     element.contentDescription?.contains(text, ignoreCase) == true
                is DetectedText -> element.text.contains(text, ignoreCase)
                is DetectedTextField -> element.hintText?.contains(text, ignoreCase) == true ||
                                        element.currentText?.contains(text, ignoreCase) == true
                is DetectedIcon -> element.resourceId?.contains(text, ignoreCase) == true
                else -> false
            }
        }
    }
    
    fun getSafeZones(): List<InteractionZone> {
        return interactionZones.filter { it.isSafe }
    }
    
    fun getDangerousZones(): List<InteractionZone> {
        return interactionZones.filter { !it.isSafe }
    }
}
