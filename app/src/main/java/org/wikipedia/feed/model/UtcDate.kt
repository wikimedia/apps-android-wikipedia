package org.wikipedia.feed.model

import java.time.LocalDate
import java.time.ZoneOffset

class UtcDate(age: Int) {
    val localDate: LocalDate = LocalDate.now(ZoneOffset.UTC).minusDays(age.toLong())
    val year get() = localDate.year.toString()
    val month get() = localDate.monthValue.toString().padStart(2, '0')
    val day get() = localDate.dayOfMonth.toString().padStart(2, '0')
}
