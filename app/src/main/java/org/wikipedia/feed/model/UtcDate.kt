package org.wikipedia.feed.model

import java.time.ZoneOffset
import java.time.ZonedDateTime

class UtcDate(private val age: Int) {
    val baseZonedDateTime: ZonedDateTime
        get() = ZonedDateTime.now(ZoneOffset.UTC).minusDays(age.toLong())

    val year get() = baseZonedDateTime.year.toString()

    val month get() = baseZonedDateTime.monthValue.toString().padStart(2, '0')

    val day get() = baseZonedDateTime.dayOfMonth.toString().padStart(2, '0')
}
