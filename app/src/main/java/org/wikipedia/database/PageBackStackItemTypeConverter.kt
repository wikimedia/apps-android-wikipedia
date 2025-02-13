package org.wikipedia.database

import androidx.room.TypeConverter
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageBackStackItem

class PageBackStackItemTypeConverter {
    @TypeConverter
    fun fromPageBackStackItem(backStacks: MutableList<PageBackStackItem>): String? {
        return JsonUtil.encodeToString(backStacks)
    }

    @TypeConverter
    fun toPageBackStackItem(backStacks: String?): MutableList<PageBackStackItem> {
        return backStacks?.let {
            JsonUtil.decodeFromString<MutableList<PageBackStackItem>>(it)
        } ?: mutableListOf()
    }
}
