package org.wikipedia.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class LocalDateTimeTypeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return if (value == null) null else LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)
    }

    @TypeConverter
    fun dateToTimestamp(dateTime: LocalDateTime?): Long? {
        return dateTime?.atOffset(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }
}
