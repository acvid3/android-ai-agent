package com.androidaiagent.perception

import com.androidaiagent.ui.model.UiMap
import java.util.LinkedHashMap

data class CachedPerception(
    val uiMap: UiMap,
    val storedAt: Long,
    val hitCount: Int = 0
)

class PerceptionCache(
    private val maxEntries: Int = 20,
    private val ttlMs: Long = 2_000L
) {
    private val cache = LinkedHashMap<String, CachedPerception>(maxEntries, 0.75f, true)

    fun get(screenshotHash: String, now: Long = System.currentTimeMillis()): UiMap? {
        val entry = cache[screenshotHash] ?: return null
        if (now - entry.storedAt > ttlMs) {
            cache.remove(screenshotHash)
            return null
        }
        cache[screenshotHash] = entry.copy(hitCount = entry.hitCount + 1)
        return entry.uiMap
    }

    fun put(screenshotHash: String, uiMap: UiMap, now: Long = System.currentTimeMillis()) {
        cache[screenshotHash] = CachedPerception(uiMap, now)
        prune()
    }

    fun clear() {
        cache.clear()
    }

    fun size(): Int = cache.size

    private fun prune() {
        while (cache.size > maxEntries) {
            val firstKey = cache.entries.firstOrNull()?.key ?: break
            cache.remove(firstKey)
        }
    }
}
