package org.wikipedia.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Extension function to convert time in milliseconds to a year
 */
fun Long.toYear(zoneId: ZoneId = ZoneId.systemDefault()): Int {
    val instant = Instant.ofEpochMilli(this)
    val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)
    return zonedDateTime.year
}
