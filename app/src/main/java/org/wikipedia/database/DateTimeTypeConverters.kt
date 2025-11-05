package org.wikipedia.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object DateTimeTypeConverters {
    @TypeConverter
    fun timestampToDateTime(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
    }

    @TypeConverter
    fun dateTimeToTimestamp(dateTime: LocalDateTime?): Long? {
        return dateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun timestampToDate(value: Long?): LocalDate? {
        return timestampToDateTime(value)?.toLocalDate()
    }

    @TypeConverter
    fun dateToTimestamp(value: LocalDate?): Long? {
        return dateTimeToTimestamp(value?.atStartOfDay())
    }
}
