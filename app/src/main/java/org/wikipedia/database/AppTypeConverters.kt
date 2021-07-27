package org.wikipedia.database

import androidx.room.TypeConverter
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import java.time.Instant
import java.util.*

object AppTypeConverters {
    @TypeConverter
    fun timestampToInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun timestampToDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
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
