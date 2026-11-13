package org.wikipedia.history.db

import androidx.room.Embedded

data class HistoryEntryWithVisitCount(
    @Embedded val entryWithImage: HistoryEntryWithImage,
    val visitCount: Int
)
