package org.wikipedia.history.db

import java.time.Instant

data class HistoryEntryWithImage(
    val authority: String,
    val lang: String,
    val apiTitle: String,
    val displayTitle: String,
    val namespace: String,
    val timestamp: Instant,
    val source: Int,
    val timeSpentSec: Int,
    val imageName: String?
)
