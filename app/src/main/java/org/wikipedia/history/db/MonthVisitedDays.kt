package org.wikipedia.history.db

data class MonthVisitedDays(
    // 1 = January, as in java.time.Month
    val month: Int,
    val visitedDays: Int
)
