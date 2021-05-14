package org.wikipedia.feed.model

import java.util.Calendar
import java.util.TimeZone

class UtcDate(private val age: Int) {

    val baseCalendar: Calendar
        get() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).also {
            it.add(Calendar.DATE, -age)
        }

    val year: String
        get() = baseCalendar[Calendar.YEAR].toString()

    // Month in String format, if its text is single digit, then add '0' at the beginning
    val month: String
        get() = (baseCalendar[Calendar.MONTH] + 1).toString().padStart(2, '0')

    // Date in String format, if its text is single digit, then add '0' at the beginning
    val date: String
        get() = baseCalendar[Calendar.DATE].toString().padStart(2, '0')
}
