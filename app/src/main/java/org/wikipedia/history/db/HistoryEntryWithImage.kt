package org.wikipedia.history.db

import java.util.*

data class HistoryEntryWithImage(
    val authority: String,
    val lang: String,
    val apiTitle: String,
    val displayTitle: String,
    val namespace: String?,
    val timestamp: Date,
    val source: Int,
    val timeSpentSec: Int,
    val imageName: String?
)
