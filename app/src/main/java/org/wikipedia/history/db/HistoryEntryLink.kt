package org.wikipedia.history.db

data class HistoryEntryLink(
    val id: Long,
    // The id of the entry this one was reached from, or -1 when it wasn't reached from another entry.
    val prevId: Long
)
