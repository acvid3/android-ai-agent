package com.androidaiagent.tracking

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class UserActionRecord(
    val appPackage: String,
    val timestamp: Long,
    val actionType: String,
    val uiContext: String?,
    val text: String? = null
)

object UserActionTracker {
    private const val REPEAT_THRESHOLD = 3
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lock = Any()
    private val history = ConcurrentHashMap<String, MutableList<UserActionRecord>>()
    private var appContext: Context? = null
    private var logFile: File? = null
    private var initialized = false

    fun init(context: Context) {
        val appCtx = context.applicationContext
        if (initialized && appContext === appCtx) return

        synchronized(lock) {
            if (initialized && appContext === appCtx) return
            appContext = appCtx
            val dir = File(appCtx.filesDir, "tracking")
            dir.mkdirs()
            logFile = File(dir, "user_actions.jsonl")
            loadFromDisk()
            initialized = true
        }
    }

    fun record(record: UserActionRecord) {
        initOrThrow()
        synchronized(lock) {
            val actions = history.getOrPut(record.appPackage) { mutableListOf() }
            actions.add(record)
            if (actions.size > 200) {
                actions.removeAt(0)
            }
            persist(record)
            detectPattern(record.appPackage)
        }
    }

    fun getRecentActions(appPackage: String, limit: Int = 20): List<UserActionRecord> {
        initOrThrow()
        return synchronized(lock) {
            history[appPackage].orEmpty().takeLast(limit)
        }
    }

    private fun detectPattern(appPackage: String) {
        val actions = history[appPackage].orEmpty()
        if (actions.size < REPEAT_THRESHOLD) return

        val recent = actions.takeLast(REPEAT_THRESHOLD)
        val actionType = recent.first().actionType
        val uiContext = recent.first().uiContext
        val repeated = recent.all { it.actionType == actionType && it.uiContext == uiContext }
        if (!repeated) return

        PatternSuggestionStore.showSuggestion(
            PatternSuggestion(
                appPackage = appPackage,
                actionType = actionType,
                repeatCount = REPEAT_THRESHOLD,
                uiContext = uiContext
            )
        )
    }

    private fun persist(record: UserActionRecord) {
        val file = logFile ?: return
        scope.launch {
            try {
                file.appendText(JSONObject().apply {
                    put("appPackage", record.appPackage)
                    put("timestamp", record.timestamp)
                    put("actionType", record.actionType)
                    put("uiContext", record.uiContext)
                    put("text", record.text)
                }.toString() + "\n")
            } catch (_: Exception) {
            }
        }
    }

    private fun loadFromDisk() {
        val file = logFile ?: return
        if (!file.exists()) return
        history.clear()
        file.forEachLine { line ->
            runCatching {
                val json = JSONObject(line)
                val record = UserActionRecord(
                    appPackage = json.optString("appPackage"),
                    timestamp = json.optLong("timestamp"),
                    actionType = json.optString("actionType"),
                    uiContext = json.optString("uiContext").takeIf { it.isNotBlank() },
                    text = json.optString("text").takeIf { it.isNotBlank() }
                )
                history.getOrPut(record.appPackage) { mutableListOf() }.add(record)
            }
        }
    }

    private fun initOrThrow() {
        if (!initialized) {
            throw IllegalStateException("UserActionTracker not initialized")
        }
    }
}
