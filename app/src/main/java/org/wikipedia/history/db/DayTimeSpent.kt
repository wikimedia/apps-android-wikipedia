package org.wikipedia.history.db

data class DayTimeSpent(
    // yyyy-MM-dd, in local time
    val day: String,
    val timeSpentSec: Int
)
