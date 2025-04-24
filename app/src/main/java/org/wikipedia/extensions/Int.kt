package org.wikipedia.extensions

import java.time.LocalDate
import java.time.ZoneId

fun Int.startOfYearInMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val localDate = LocalDate.of(this, 1, 1)
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun Int.endOfYearInMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val localDate = LocalDate.of(this, 12, 31)
    return localDate.atTime(0, 0, 0).atZone(zoneId).toInstant().toEpochMilli()
}
