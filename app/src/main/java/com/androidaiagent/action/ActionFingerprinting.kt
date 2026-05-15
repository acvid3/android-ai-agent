package com.androidaiagent.action

import com.androidaiagent.ai.ActionType
import com.androidaiagent.ui.model.UiMap
import java.security.MessageDigest
import java.util.ArrayDeque

data class ActionFingerprint(
    val action: ActionType,
    val target: String?,
    val route: String?,
    val screenHash: String?,
    val parametersHash: String
)

class ActionFingerprinting {
    private val recentFingerprints = ArrayDeque<String>()
    private val maxRecentFingerprints = 50

    fun fingerprint(action: ActionType, target: String?, route: String?, uiMap: UiMap?, parameters: Map<String, Any>): ActionFingerprint {
        val screenHash = uiMap?.currentScreen?.screenshotHash
        val parametersHash = hashParameters(parameters)
        return ActionFingerprint(action, target, route, screenHash, parametersHash)
    }

    fun shouldDeduplicate(fingerprint: ActionFingerprint): Boolean {
        val key = fingerprintKey(fingerprint)
        if (recentFingerprints.contains(key)) {
            return true
        }
        recentFingerprints.addLast(key)
        while (recentFingerprints.size > maxRecentFingerprints) {
            recentFingerprints.removeFirst()
        }
        return false
    }

    fun clear() {
        recentFingerprints.clear()
    }

    private fun fingerprintKey(fingerprint: ActionFingerprint): String {
        return listOf(
            fingerprint.action.name,
            fingerprint.target.orEmpty(),
            fingerprint.route.orEmpty(),
            fingerprint.screenHash.orEmpty(),
            fingerprint.parametersHash
        ).joinToString("|")
    }

    private fun hashParameters(parameters: Map<String, Any>): String {
        if (parameters.isEmpty()) return "empty"
        val digest = MessageDigest.getInstance("SHA-256")
        val sorted = parameters.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }
        val bytes = digest.digest(sorted.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
