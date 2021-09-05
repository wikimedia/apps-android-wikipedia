package org.wikipedia.database

import androidx.room.TypeConverter
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

object AppTypeConverters {
    @TypeConverter
    fun timestampToDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun timestampToLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    }

    @TypeConverter
    fun localDateTimeToTimestamp(localDateTime: LocalDateTime?): Long? {
        return localDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun intToNamespace(value: Int?): Namespace? {
        return value?.let { Namespace.of(it) }
    }

    @TypeConverter
    fun namespaceToInt(ns: Namespace?): Int? {
        return ns?.code()
    }

    @TypeConverter
    fun stringToWikiSite(value: String?): WikiSite? {
        return value?.let { WikiSite(it) }
    }

    @TypeConverter
    fun wikiSiteToString(wikiSite: WikiSite?): String? {
        return wikiSite?.authority()
    }
}
