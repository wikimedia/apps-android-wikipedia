package org.wikipedia.history.db

import androidx.room.Embedded

data class HistoryEntryWithTimeSpent(
    @Embedded val entryWithImage: HistoryEntryWithImage,
    // Unlike entryWithImage.timeSpentSec, which is the article's total across all of its visits, only the part credited to the queried range.
    val creditedTimeSpentSec: Int
)
