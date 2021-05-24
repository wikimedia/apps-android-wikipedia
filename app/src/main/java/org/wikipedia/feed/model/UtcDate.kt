package org.wikipedia.feed.model

import java.util.Calendar
import java.util.TimeZone

class UtcDate(private val age: Int) {

    val baseCalendar: Calendar
        get() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).also {
            it.add(Calendar.DATE, -age)
        }

    val year get() = baseCalendar[Calendar.YEAR].toString()

    val month get() = (baseCalendar[Calendar.MONTH] + 1).toString().padStart(2, '0')

    val day get() = baseCalendar[Calendar.DATE].toString().padStart(2, '0')
}
