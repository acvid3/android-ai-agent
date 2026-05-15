package com.androidaiagent.routing

data class RouteTransactionEntry(
    val route: String?,
    val targetRoute: String?,
    val startedAt: Long,
    val completedAt: Long? = null,
    val success: Boolean? = null,
    val reason: String? = null
)

class RouteTransactionJournal(
    private val maxEntries: Int = 100
) {
    private val entries = ArrayList<RouteTransactionEntry>()

    fun begin(route: String?, targetRoute: String?): Int {
        entries.add(RouteTransactionEntry(route, targetRoute, System.currentTimeMillis()))
        prune()
        return entries.lastIndex
    }

    fun commit(index: Int, success: Boolean, reason: String? = null) {
        if (index !in entries.indices) return
        val entry = entries[index]
        entries[index] = entry.copy(
            completedAt = System.currentTimeMillis(),
            success = success,
            reason = reason
        )
    }

    fun recent(): List<RouteTransactionEntry> = entries.toList()

    fun clear() {
        entries.clear()
    }

    private fun prune() {
        while (entries.size > maxEntries) {
            entries.removeAt(0)
        }
    }
}
