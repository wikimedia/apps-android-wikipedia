package org.wikipedia.history.db

data class HourVisitCount(
    // 0 to 23, in local time
    val hour: Int,
    val visitCount: Int
)
