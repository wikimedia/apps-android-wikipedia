package org.wikipedia.extensions

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Date.isToday(): Boolean {
    return this.toLocalDate() == LocalDate.now()
}

fun Date.isYesterday(): Boolean {
    return this.toLocalDate() == LocalDate.now().minusDays(1)
}
