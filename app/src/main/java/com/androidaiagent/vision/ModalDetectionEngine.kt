package com.androidaiagent.vision

import com.androidaiagent.ui.model.UiMap

class ModalDetectionEngine {
    fun detect(uiMap: UiMap): ModalDetectionResult {
        val screen = uiMap.currentScreen
        val text = screen.textElements.joinToString(" ") { it.text }
        val buttonLabels = screen.buttons.mapNotNull { it.text ?: it.contentDescription }

        val kind = when {
            buttonLabels.any { it.contains("allow", ignoreCase = true) || it.contains("permission", ignoreCase = true) } ->
                ModalKind.PERMISSION
            buttonLabels.any { it.contains("update", ignoreCase = true) || text.contains("update available", ignoreCase = true) } ->
                ModalKind.UPDATE
            buttonLabels.any { it.contains("ad", ignoreCase = true) || text.contains("sponsored", ignoreCase = true) } ->
                ModalKind.AD
            buttonLabels.any { it.contains("ok", ignoreCase = true) || it.contains("confirm", ignoreCase = true) || it.contains("yes", ignoreCase = true) } ->
                ModalKind.CONFIRMATION
            buttonLabels.any { it.contains("close", ignoreCase = true) || it.contains("dismiss", ignoreCase = true) } ->
                ModalKind.DIALOG
            else -> ModalKind.NONE
        }

        val severity = when (kind) {
            ModalKind.PERMISSION, ModalKind.UPDATE -> BlockingSeverity.HIGH
            ModalKind.AD -> BlockingSeverity.MEDIUM
            ModalKind.CONFIRMATION, ModalKind.DIALOG -> BlockingSeverity.LOW
            ModalKind.NONE -> BlockingSeverity.NONE
        }

        return ModalDetectionResult(kind, severity, kind != ModalKind.NONE)
    }
}

data class ModalDetectionResult(
    val kind: ModalKind,
    val severity: BlockingSeverity,
    val blocking: Boolean
)

enum class ModalKind {
    NONE,
    DIALOG,
    PERMISSION,
    UPDATE,
    AD,
    CONFIRMATION
}

enum class BlockingSeverity {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}
