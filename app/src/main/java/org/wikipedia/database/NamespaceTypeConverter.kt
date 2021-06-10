package org.wikipedia.database

import androidx.room.TypeConverter
import org.wikipedia.page.Namespace

class NapespaceTypeConverter {
    @TypeConverter
    fun intToNamespace(value: Int?): Namespace? {
        return if (value == null) null else Namespace.of(value)
    }

    @TypeConverter
    fun namespaceToInt(ns: Namespace?): Int? {
        return ns?.code()
    }
}
