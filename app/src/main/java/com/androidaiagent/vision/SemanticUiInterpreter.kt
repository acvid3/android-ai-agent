package com.androidaiagent.vision

import com.androidaiagent.ui.model.DetectedButton
import com.androidaiagent.ui.model.DetectedText
import com.androidaiagent.ui.model.DetectedTextField
import com.androidaiagent.ui.model.InteractionZone
import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.ui.model.UiScreen
import android.graphics.Rect

class SemanticUiInterpreter {
    fun interpret(uiMap: UiMap): SemanticUiContext {
        val screen = uiMap.currentScreen
        val buttons = screen.buttons
        val textFields = screen.textFields
        val text = screen.textElements
        val modalDetected = detectModal(screen)

        return SemanticUiContext(
            navigationZones = buildNavigationZones(screen),
            interactionClusters = buildInteractionClusters(buttons, textFields, text),
            modalDetected = modalDetected,
            primaryAction = inferPrimaryAction(buttons),
            secondaryAction = inferSecondaryAction(buttons),
            summary = summarize(screen, modalDetected)
        )
    }

    private fun detectModal(screen: UiScreen): Boolean {
        val hasClose = screen.buttons.any { isCloseLike(it) }
        val zoneCount = screen.interactionZones.size
        return hasClose && zoneCount <= 2 && screen.buttons.size <= 4
    }

    private fun buildNavigationZones(screen: UiScreen): List<InteractionCluster> {
        return screen.buttons
            .filter { isNavigationLike(it) }
            .map { button ->
                InteractionCluster(
                    label = "navigation",
                    bounds = button.bounds,
                    confidence = button.confidence,
                    elements = listOf(button.text ?: button.contentDescription ?: "nav")
                )
            }
    }

    private fun buildInteractionClusters(
        buttons: List<DetectedButton>,
        textFields: List<DetectedTextField>,
        text: List<DetectedText>
    ): List<InteractionCluster> {
        val clusters = mutableListOf<InteractionCluster>()

        val primaryButtons = buttons.filter { isPrimaryLike(it) }
        if (primaryButtons.isNotEmpty()) {
            clusters += InteractionCluster(
                label = "primary_actions",
                bounds = unionBounds(primaryButtons.map { it.bounds }),
                confidence = primaryButtons.maxOf { it.confidence },
                elements = primaryButtons.mapNotNull { it.text ?: it.contentDescription }
            )
        }

        if (textFields.isNotEmpty()) {
            clusters += InteractionCluster(
                label = "input_fields",
                bounds = unionBounds(textFields.map { it.bounds }),
                confidence = textFields.maxOf { it.confidence },
                elements = textFields.mapNotNull { it.hintText ?: it.currentText }
            )
        }

        if (text.isNotEmpty()) {
            clusters += InteractionCluster(
                label = "content",
                bounds = unionBounds(text.map { it.bounds }),
                confidence = text.maxOf { it.confidence },
                elements = text.map { it.text }.distinct().take(8)
            )
        }

        return clusters
    }

    private fun summarize(screen: UiScreen, modalDetected: Boolean): String {
        val summaryBits = mutableListOf<String>()
        summaryBits += "pkg=${screen.packageName}"
        summaryBits += "activity=${screen.activityName ?: "unknown"}"
        summaryBits += "buttons=${screen.buttons.size}"
        summaryBits += "texts=${screen.textElements.size}"
        summaryBits += "fields=${screen.textFields.size}"
        if (modalDetected) summaryBits += "modal=true"
        return summaryBits.joinToString(",")
    }

    private fun inferPrimaryAction(buttons: List<DetectedButton>): String? {
        val primary = buttons.firstOrNull { isPrimaryLike(it) }
        return primary?.text ?: primary?.contentDescription
    }

    private fun inferSecondaryAction(buttons: List<DetectedButton>): String? {
        val secondary = buttons.firstOrNull { isSecondaryLike(it) }
        return secondary?.text ?: secondary?.contentDescription
    }

    private fun isNavigationLike(button: DetectedButton): Boolean {
        val value = button.text.orEmpty() + " " + button.contentDescription.orEmpty()
        return value.contains("back", ignoreCase = true) ||
            value.contains("menu", ignoreCase = true) ||
            value.contains("home", ignoreCase = true) ||
            value.contains("next", ignoreCase = true)
    }

    private fun isPrimaryLike(button: DetectedButton): Boolean {
        val value = button.text.orEmpty() + " " + button.contentDescription.orEmpty()
        return value.contains("continue", ignoreCase = true) ||
            value.contains("save", ignoreCase = true) ||
            value.contains("confirm", ignoreCase = true) ||
            value.contains("ok", ignoreCase = true) ||
            value.contains("submit", ignoreCase = true)
    }

    private fun isSecondaryLike(button: DetectedButton): Boolean {
        val value = button.text.orEmpty() + " " + button.contentDescription.orEmpty()
        return value.contains("cancel", ignoreCase = true) ||
            value.contains("close", ignoreCase = true) ||
            value.contains("later", ignoreCase = true)
    }

    private fun isCloseLike(button: DetectedButton): Boolean {
        val value = button.text.orEmpty() + " " + button.contentDescription.orEmpty()
        return value.contains("close", ignoreCase = true) ||
            value.contains("dismiss", ignoreCase = true) ||
            value.contains("x", ignoreCase = true)
    }

    private fun unionBounds(bounds: List<Rect>): Rect {
        if (bounds.isEmpty()) return Rect()
        val left = bounds.minOf { it.left }
        val top = bounds.minOf { it.top }
        val right = bounds.maxOf { it.right }
        val bottom = bounds.maxOf { it.bottom }
        return Rect(left, top, right, bottom)
    }
}

data class SemanticUiContext(
    val navigationZones: List<InteractionCluster>,
    val interactionClusters: List<InteractionCluster>,
    val modalDetected: Boolean,
    val primaryAction: String?,
    val secondaryAction: String?,
    val summary: String
)

data class InteractionCluster(
    val label: String,
    val bounds: Rect,
    val confidence: Float,
    val elements: List<String>
)
