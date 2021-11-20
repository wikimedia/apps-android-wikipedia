package org.wikipedia.database

import androidx.room.TypeConverter
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification

class NotificationTypeConverters {
    @TypeConverter
    fun stringToContents(value: String?): Notification.Contents? {
        return JsonUtil.decodeFromString<Notification.Contents>(value)
    }

    @TypeConverter
    fun contentsToString(contents: Notification.Contents?): String? {
        return JsonUtil.encodeToString(contents)
    }

    @TypeConverter
    fun stringToTimestamp(value: String?): Notification.Timestamp? {
        return JsonUtil.decodeFromString<Notification.Timestamp>(value)
    }

    @TypeConverter
    fun timestampToString(timestamp: Notification.Timestamp?): String? {
        return JsonUtil.encodeToString(timestamp)
    }

    @TypeConverter
    fun stringToTitle(value: String?): Notification.Title? {
        return JsonUtil.decodeFromString<Notification.Title>(value)
    }

    @TypeConverter
    fun titleToString(title: Notification.Title?): String? {
        return JsonUtil.encodeToString(title)
    }

    @TypeConverter
    fun stringToAgent(value: String?): Notification.Agent? {
        return JsonUtil.decodeFromString<Notification.Agent>(value)
    }

    @TypeConverter
    fun agentToString(agent: Notification.Agent?): String? {
        return JsonUtil.encodeToString(agent)
    }
}
