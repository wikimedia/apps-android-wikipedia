package org.wikipedia.feed.model

import java.util.Calendar
import java.util.TimeZone

class UtcDate(age: Int) {
    private val cal: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val year: String
    private val month: String
    private val date: String

    fun baseCalendar(): Calendar {
        return cal
    }

    fun year(): String {
        return year
    }

    fun month(): String {
        return month
    }

    fun date(): String {
        return date
    }

    init {
        cal.add(Calendar.DATE, -age)
        year = cal[Calendar.YEAR].toString()
        month = pad((cal[Calendar.MONTH] + 1).toString())
        date = pad(cal[Calendar.DATE].toString())
    }

    companion object {
        private fun pad(value: String): String {
            return if (value.length == 1) {
                "0$value"
            } else value
        }
    }
}