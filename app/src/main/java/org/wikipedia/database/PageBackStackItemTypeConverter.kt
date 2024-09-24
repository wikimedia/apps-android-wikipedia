package org.wikipedia.database

import androidx.room.TypeConverter
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageBackStackItem

class PageBackStackItemTypeConverter {
    @TypeConverter
    fun fromBackStack(tabs: List<PageBackStackItem>): String? {
        return JsonUtil.encodeToString(tabs)
    }

    @TypeConverter
    fun toBackStack(tabs: String?): List<PageBackStackItem> {
        return tabs?.let {
            JsonUtil.decodeFromString<List<PageBackStackItem>>(it)
        } ?: emptyList()
    }
}
