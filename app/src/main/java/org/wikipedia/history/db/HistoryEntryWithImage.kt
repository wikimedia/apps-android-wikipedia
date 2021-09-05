package org.wikipedia.history.db

import java.time.LocalDateTime

data class HistoryEntryWithImage(
    val authority: String,
    val lang: String,
    val apiTitle: String,
    val displayTitle: String,
    val namespace: String,
    val timestamp: LocalDateTime,
    val source: Int,
    val timeSpentSec: Int,
    val imageName: String?
)
